import SwiftUI

enum TmdbColor {
    static let primary = Color(hex: 0x8000FF)
    static let onPrimary = Color(hex: 0xFFFFFF)
    static let primaryContainer = Color(hex: 0x540BA1)
    static let onPrimaryContainer = Color(hex: 0xFFFFFF)
    static let inversePrimary = Color(hex: 0x6A00D6)
    static let secondary = Color(hex: 0xFF1F8A)
    static let onSecondary = Color(hex: 0xFFFFFF)
    static let secondaryContainer = Color(hex: 0x381A39)
    static let onSecondaryContainer = Color(hex: 0xFFB1C8)
    static let tertiary = Color(hex: 0xC9A7FF)
    static let onTertiary = Color(hex: 0x2A0059)
    static let tertiaryContainer = Color(hex: 0x36076B)
    static let onTertiaryContainer = Color(hex: 0xEBDDFF)
    static let background = Color(hex: 0x130E1F)
    static let onBackground = Color(hex: 0xFFFFFF)
    static let surface = Color(hex: 0x130E1F)
    static let onSurface = Color(hex: 0xFFFFFF)
    static let surfaceVariant = Color(hex: 0x36076B)
    static let onSurfaceVariant = Color(hex: 0xA6A1B5)
    static let surfaceTint = Color(hex: 0x8000FF)
    static let inverseSurface = Color(hex: 0xE8E0F0)
    static let inverseOnSurface = Color(hex: 0x211A30)
    static let error = Color(hex: 0xFFB4AB)
    static let onError = Color(hex: 0x690005)
    static let errorContainer = Color(hex: 0x93000A)
    static let onErrorContainer = Color(hex: 0xFFDAD6)
    static let outline = Color(hex: 0x655F74)
    static let outlineVariant = Color(hex: 0x3C3549)
    static let scrim = Color(hex: 0x000000)
    static let surfaceBright = Color(hex: 0x3A3347)
    static let surfaceContainer = Color(hex: 0x211A30)
    static let surfaceContainerHigh = Color(hex: 0x312A3F)
    static let surfaceContainerHighest = Color(hex: 0x3C3549)
    static let surfaceContainerLow = Color(hex: 0x1A1428)
    static let surfaceContainerLowest = Color(hex: 0x0E0A17)
    static let surfaceDim = Color(hex: 0x130E1F)
}

private extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}
