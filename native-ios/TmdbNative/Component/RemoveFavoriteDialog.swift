import SwiftUI

private let dialogColor = Color(red: 0x21 / 255, green: 0x1A / 255, blue: 0x30 / 255)
private let messageColor = Color(red: 0xB0 / 255, green: 0xAA / 255, blue: 0xBF / 255)
private let cancelBorderColor = Color(red: 0x65 / 255, green: 0x5F / 255, blue: 0x74 / 255)
private let scrimOpacity = 0.68
private let screenMargin: CGFloat = 20
private let contentPadding: CGFloat = 24
private let bottomPadding: CGFloat = 20
private let titleToMessage: CGFloat = 20
private let messageToButtons: CGFloat = 24
private let buttonSpacing: CGFloat = 12
private let dialogRadius: CGFloat = 20
private let buttonHeight: CGFloat = 48
private let buttonRadius: CGFloat = 10

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

    @ScaledMetric(relativeTo: .title2) private var titleSize: CGFloat = 22
    @ScaledMetric(relativeTo: .subheadline) private var messageSize: CGFloat = 14
    @ScaledMetric(relativeTo: .body) private var buttonSize: CGFloat = 16

    var body: some View {
        ZStack {
            Color.black.opacity(scrimOpacity)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)
                .accessibilityHidden(true)
            ViewThatFits(in: .vertical) {
                content
                ScrollView { content }
            }
            .background(dialogColor, in: RoundedRectangle(cornerRadius: dialogRadius))
            .padding(.horizontal, screenMargin)
            .accessibilityElement(children: .contain)
            .accessibilityAddTraits(.isModal)
            .accessibilityAction(.escape, onDismiss)
        }
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("removeFavorite.title")
                .font(.system(size: titleSize, weight: .bold))
                .foregroundStyle(.white)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            message
                .font(.system(size: messageSize))
                .lineSpacing(6)
                .foregroundStyle(messageColor)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, titleToMessage)
            HStack(spacing: buttonSpacing) {
                Button(action: onDismiss) {
                    Text("removeFavorite.cancel")
                        .frame(maxWidth: .infinity, minHeight: buttonHeight)
                        .overlay(RoundedRectangle(cornerRadius: buttonRadius).stroke(cancelBorderColor, lineWidth: 1))
                        .contentShape(Rectangle())
                }
                Button(action: onConfirm) {
                    Text("removeFavorite.confirm")
                        .frame(maxWidth: .infinity, minHeight: buttonHeight)
                        .background(Theme.accent, in: RoundedRectangle(cornerRadius: buttonRadius))
                }
            }
            .buttonStyle(.plain)
            .font(.system(size: buttonSize, weight: .semibold))
            .foregroundStyle(.white)
            .padding(.top, messageToButtons)
        }
        .padding(.horizontal, contentPadding)
        .padding(.top, contentPadding)
        .padding(.bottom, bottomPadding)
    }

    private var message: Text {
        if let titleName {
            Text("removeFavorite.message \(titleName)")
        } else {
            Text("removeFavorite.messageUnknown")
        }
    }
}
