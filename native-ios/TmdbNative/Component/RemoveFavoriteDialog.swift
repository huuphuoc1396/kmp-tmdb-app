import SwiftUI

extension View {
    func removeFavoriteDialog(
        isPresented: Bool,
        titleName: String?,
        onConfirm: @escaping () -> Void,
        onDismiss: @escaping () -> Void
    ) -> some View {
        overlay {
            if isPresented {
                RemoveFavoriteDialog(titleName: titleName, onConfirm: onConfirm, onDismiss: onDismiss)
            }
        }
    }
}

private struct RemoveFavoriteDialog: View {
    let titleName: String?
    let onConfirm: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            TmdbColor.scrim.opacity(0.68)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)
                .accessibilityHidden(true)
            ViewThatFits(in: .vertical) {
                content
                ScrollView { content }
            }
            .background(TmdbColor.surfaceContainer, in: RoundedRectangle(cornerRadius: TmdbShape.extraLarge))
            .padding(.horizontal, TmdbSpacing.lg)
            .accessibilityElement(children: .contain)
            .accessibilityAddTraits(.isModal)
            .accessibilityAction(.escape, onDismiss)
        }
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("removeFavorite.title")
                .tmdbFont(.titleLarge)
                .foregroundStyle(TmdbColor.onSurface)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            message
                .tmdbFont(.bodyMedium)
                .foregroundStyle(TmdbColor.onSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, TmdbSpacing.lg)
            HStack(spacing: TmdbSpacing.sm) {
                Button(action: onDismiss) {
                    Text("removeFavorite.cancel")
                        .foregroundStyle(TmdbColor.onSurface)
                        .frame(maxWidth: .infinity, minHeight: TmdbSize.touchTarget)
                        .overlay(
                            RoundedRectangle(cornerRadius: TmdbShape.medium)
                                .stroke(TmdbColor.outline, lineWidth: TmdbSize.borderWidth)
                        )
                        .contentShape(Rectangle())
                }
                Button(action: onConfirm) {
                    Text("removeFavorite.confirm")
                        .foregroundStyle(TmdbColor.onSecondary)
                        .frame(maxWidth: .infinity, minHeight: TmdbSize.touchTarget)
                        .background(TmdbColor.secondary, in: RoundedRectangle(cornerRadius: TmdbShape.medium))
                }
            }
            .buttonStyle(.plain)
            .tmdbFont(.titleMedium)
            .padding(.top, TmdbSpacing.xl)
        }
        .padding(.horizontal, TmdbSpacing.xl)
        .padding(.top, TmdbSpacing.xl)
        .padding(.bottom, TmdbSpacing.lg)
    }

    private var message: Text {
        if let titleName {
            Text("removeFavorite.message \(titleName)")
        } else {
            Text("removeFavorite.messageUnknown")
        }
    }
}
