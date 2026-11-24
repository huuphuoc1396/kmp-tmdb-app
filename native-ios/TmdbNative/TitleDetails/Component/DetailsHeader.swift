import SwiftUI
import TmdbCore

struct DetailsHeader: View {
    let details: TitleDetailsItem
    let language: Language

    var body: some View {
        HStack(alignment: .top, spacing: TmdbSpacing.md) {
            RatingRing(percent: details.ratingPercent)
            VStack(alignment: .leading, spacing: TmdbSpacing.xxs) {
                NameText(
                    TitleDetailsFormat.nameWithYear(details.name, year: details.releaseDate?.year),
                    style: .titleLarge,
                    lineLimit: nil
                )
                .foregroundStyle(TmdbColor.onSurface)
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

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 0) {
                if let date { dateText(date) }
                if date != nil, runtime != nil {
                    Circle()
                        .fill(TmdbColor.onSurfaceVariant)
                        .frame(width: 4, height: 4)
                        .padding(.leading, TmdbSpacing.xxs)
                        .padding(.trailing, TmdbSpacing.xs)
                }
                if let runtime { runtimeLabel(runtime) }
            }
            VStack(alignment: .leading, spacing: TmdbSpacing.xxs) {
                if let date { dateText(date) }
                if let runtime { runtimeLabel(runtime) }
            }
        }
        .tmdbFont(.bodyLarge)
        .foregroundStyle(TmdbColor.onSurfaceVariant)
    }

    private func dateText(_ date: String) -> some View {
        Text(verbatim: date).fixedSize()
    }

    private func runtimeLabel(_ runtime: String) -> some View {
        HStack(spacing: TmdbSpacing.xs) {
            Image("Clock")
                .resizable()
                .frame(width: TmdbSize.iconSmall, height: TmdbSize.iconSmall)
                .accessibilityHidden(true)
            Text(verbatim: runtime).fixedSize()
        }
    }
}
