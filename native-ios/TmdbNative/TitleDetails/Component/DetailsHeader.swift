import SwiftUI
import TmdbCore

private let ringToName: CGFloat = 16
private let nameToDateLine: CGFloat = 5

struct DetailsHeader: View {
    let details: TitleDetailsItem
    let language: Language

    @ScaledMetric(relativeTo: .title2) private var nameSize: CGFloat = 22

    var body: some View {
        HStack(alignment: .top, spacing: ringToName) {
            RatingRing(percent: details.ratingPercent)
            VStack(alignment: .leading, spacing: nameToDateLine) {
                Text(verbatim: TitleDetailsFormat.nameWithYear(details.name, year: details.releaseDate?.year))
                    .font(.system(size: nameSize, weight: .bold))
                    .foregroundStyle(.white)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityAddTraits(.isHeader)
                let date = details.releaseDate.map { TitleDetailsFormat.releaseDate($0, language: language) }
                let runtime = details.runtimeMinutes.map(TitleDetailsFormat.runtime(minutes:))
                if date != nil || runtime != nil {
                    DateLine(date: date, runtime: runtime)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private struct DateLine: View {
    let date: String?
    let runtime: String?

    @ScaledMetric(relativeTo: .body) private var textSize: CGFloat = 16
    @ScaledMetric(relativeTo: .body) private var clockSize: CGFloat = 19

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 0) {
                if let date { dateText(date) }
                if date != nil, runtime != nil {
                    Circle()
                        .fill(Theme.dateLine)
                        .frame(width: 4, height: 4)
                        .padding(.leading, 3)
                        .padding(.trailing, 6)
                }
                if let runtime { runtimeLabel(runtime) }
            }
            VStack(alignment: .leading, spacing: 4) {
                if let date { dateText(date) }
                if let runtime { runtimeLabel(runtime) }
            }
        }
        .font(.system(size: textSize))
        .foregroundStyle(Theme.dateLine)
    }

    private func dateText(_ date: String) -> some View {
        Text(verbatim: date).fixedSize()
    }

    private func runtimeLabel(_ runtime: String) -> some View {
        HStack(spacing: 6) {
            Image("Clock")
                .resizable()
                .frame(width: clockSize, height: clockSize)
                .accessibilityHidden(true)
            Text(verbatim: runtime).fixedSize()
        }
    }
}
