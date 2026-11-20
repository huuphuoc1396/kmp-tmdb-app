import Foundation
import TmdbCore

/// Shows both lists of the selected category in the device language. Lists refresh when shown (stale ones only),
/// on pull-to-refresh and on retry (always).
@MainActor
final class HomeViewModel: ObservableObject {
    @Published private(set) var state = HomeUiState()

    /// A new stream for each collector, which replaces the previous one: the view collects again each time it
    /// reappears (after details close), and a cancelled `AsyncStream` can't be iterated again.
    var effects: AsyncStream<HomeEffect> {
        let (stream, continuation) = AsyncStream.makeStream(of: HomeEffect.self)
        effectsContinuation?.finish()
        effectsContinuation = continuation
        return stream
    }

    private let titleLists: any TitleLists
    private var effectsContinuation: AsyncStream<HomeEffect>.Continuation?

    /// `nil` until the view reports the device language; nothing is observed or refreshed before that.
    private var language: Language?
    /// Latest values of the current selection's lists; a missing entry means never loaded.
    private var observed: [TitleListType: [TitleItem]] = [:]
    /// Lists whose last refresh failed, per category and language, so results never leak into another tab.
    private var failed: Set<TitleListKey> = []
    private let observations = TaskBag()
    private var pullRefresh: Task<Void, Never>?

    init(titleLists: any TitleLists) {
        self.titleLists = titleLists
    }

    func onIntent(_ intent: HomeIntent) {
        switch intent {
        case let .languageChanged(language):
            changeLanguage(language)
        case let .categorySelected(category):
            selectCategory(category)
        case let .titleTapped(title):
            effectsContinuation?.yield(.openTitle(title))
        case let .retry(listType):
            retry(listType)
        }
    }

    /// Pull-to-refresh: forces both lists of the selection and returns when both end. A pull while one is
    /// running waits for it instead of starting another.
    func refresh() async {
        if let running = pullRefresh {
            await running.value
            return
        }
        guard let featured = key(.featured), let popular = key(.popular) else { return }
        state.isRefreshing = true
        let task = Task {
            async let featuredDone: Void = refreshList(featured, force: true)
            async let popularDone: Void = refreshList(popular, force: true)
            _ = await (featuredDone, popularDone)
        }
        pullRefresh = task
        await task.value
        pullRefresh = nil
        state.isRefreshing = false
    }

    private func changeLanguage(_ newLanguage: Language) {
        guard newLanguage != language else { return }
        language = newLanguage
        showSelection()
    }

    private func selectCategory(_ category: TitleCategory) {
        guard category != state.selectedCategory else { return }
        state.selectedCategory = category
        showSelection()
    }

    /// Observes the selection's lists instead of the previous ones and refreshes the stale ones.
    private func showSelection() {
        observations.cancelAll()
        observed = [:]
        updateRows()
        for listType in TitleListType.allCases {
            guard let key = key(listType) else { return }
            let stream = titleLists.observe(key)
            observations.add(Task { [weak self] in
                for await titles in stream {
                    guard let self, !Task.isCancelled else { return }
                    observed[listType] = titles
                    updateRows()
                }
            })
            Task { await refreshList(key, force: false) }
        }
    }

    private func retry(_ listType: TitleListType) {
        guard let key = key(listType) else { return }
        Task { await refreshList(key, force: true) }
    }

    private func refreshList(_ key: TitleListKey, force: Bool) async {
        failed.remove(key)
        updateRows()
        if await !titleLists.refresh(key, force: force) {
            failed.insert(key)
        }
        updateRows()
    }

    private func updateRows() {
        state.featured = rowState(.featured)
        state.popular = rowState(.popular)
    }

    private func rowState(_ listType: TitleListType) -> RowState {
        if let titles = observed[listType] {
            return .titles(titles)
        }
        if let key = key(listType), failed.contains(key) {
            return .error
        }
        return .loading
    }

    private func key(_ listType: TitleListType) -> TitleListKey? {
        language.map { TitleListKey(category: state.selectedCategory, listType: listType, language: $0) }
    }
}

/// Holds the observation tasks and cancels them when the ViewModel goes away.
private final class TaskBag: @unchecked Sendable {
    private var tasks: [Task<Void, Never>] = []

    func add(_ task: Task<Void, Never>) {
        tasks.append(task)
    }

    func cancelAll() {
        tasks.forEach { $0.cancel() }
        tasks = []
    }

    deinit {
        cancelAll()
    }
}
