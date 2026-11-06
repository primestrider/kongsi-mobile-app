package com.primestrider.kotlinstarter.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/** Raw color palette ported from the React Native boilerplate. Prefer semantic [AppColors] in screens. */
object Palette {
    val white = Color(0xFFFFFFFF)
    val black = Color(0xFF000000)
    val transparent = Color(0x00000000)

    object Primary {
        val c50 = Color(0xFFE8F4FE)
        val c100 = Color(0xFFC5E4FC)
        val c200 = Color(0xFF9ED0FA)
        val c300 = Color(0xFF6BB8F7)
        val c400 = Color(0xFF3A9FF2)
        val c500 = Color(0xFF208AEF)
        val c600 = Color(0xFF1A73D4)
        val c700 = Color(0xFF155CB0)
        val c800 = Color(0xFF10478C)
        val c900 = Color(0xFF0B3368)
    }

    object Red {
        val c50 = Color(0xFFFEF2F2)
        val c100 = Color(0xFFFEE2E2)
        val c200 = Color(0xFFFECACA)
        val c300 = Color(0xFFFCA5A5)
        val c400 = Color(0xFFF87171)
        val c500 = Color(0xFFEF4444)
        val c600 = Color(0xFFDC2626)
        val c700 = Color(0xFFB91C1C)
        val c800 = Color(0xFF991B1B)
        val c900 = Color(0xFF7F1D1D)
        val c950 = Color(0xFF450A0A)
    }

    object Orange {
        val c50 = Color(0xFFFFF7ED)
        val c100 = Color(0xFFFFEDD5)
        val c200 = Color(0xFFFED7AA)
        val c300 = Color(0xFFFDBA74)
        val c400 = Color(0xFFFB923C)
        val c500 = Color(0xFFF97316)
        val c600 = Color(0xFFEA580C)
        val c700 = Color(0xFFC2410C)
        val c800 = Color(0xFF9A3412)
        val c900 = Color(0xFF7C2D12)
        val c950 = Color(0xFF431407)
    }

    object Amber {
        val c50 = Color(0xFFFFFBEB)
        val c100 = Color(0xFFFEF3C7)
        val c200 = Color(0xFFFDE68A)
        val c300 = Color(0xFFFCD34D)
        val c400 = Color(0xFFFBBF24)
        val c500 = Color(0xFFF59E0B)
        val c600 = Color(0xFFD97706)
        val c700 = Color(0xFFB45309)
        val c800 = Color(0xFF92400E)
        val c900 = Color(0xFF78350F)
        val c950 = Color(0xFF451A03)
    }

    object Yellow {
        val c50 = Color(0xFFFEFCE8)
        val c100 = Color(0xFFFEF9C3)
        val c200 = Color(0xFFFEF08A)
        val c300 = Color(0xFFFDE047)
        val c400 = Color(0xFFFACC15)
        val c500 = Color(0xFFEAB308)
        val c600 = Color(0xFFCA8A04)
        val c700 = Color(0xFFA16207)
        val c800 = Color(0xFF854D0E)
        val c900 = Color(0xFF713F12)
        val c950 = Color(0xFF422006)
    }

    object Lime {
        val c50 = Color(0xFFF7FEE7)
        val c100 = Color(0xFFECFCCB)
        val c200 = Color(0xFFD9F99D)
        val c300 = Color(0xFFBEF264)
        val c400 = Color(0xFFA3E635)
        val c500 = Color(0xFF84CC16)
        val c600 = Color(0xFF65A30D)
        val c700 = Color(0xFF4D7C0F)
        val c800 = Color(0xFF3F6212)
        val c900 = Color(0xFF365314)
        val c950 = Color(0xFF1A2E05)
    }

    object Green {
        val c50 = Color(0xFFF0FDF4)
        val c100 = Color(0xFFDCFCE7)
        val c200 = Color(0xFFBBF7D0)
        val c300 = Color(0xFF86EFAC)
        val c400 = Color(0xFF4ADE80)
        val c500 = Color(0xFF22C55E)
        val c600 = Color(0xFF16A34A)
        val c700 = Color(0xFF15803D)
        val c800 = Color(0xFF166534)
        val c900 = Color(0xFF14532D)
        val c950 = Color(0xFF052E16)
    }

    object Emerald {
        val c50 = Color(0xFFECFDF5)
        val c100 = Color(0xFFD1FAE5)
        val c200 = Color(0xFFA7F3D0)
        val c300 = Color(0xFF6EE7B7)
        val c400 = Color(0xFF34D399)
        val c500 = Color(0xFF10B981)
        val c600 = Color(0xFF059669)
        val c700 = Color(0xFF047857)
        val c800 = Color(0xFF065F46)
        val c900 = Color(0xFF064E3B)
        val c950 = Color(0xFF022C22)
    }

    object Teal {
        val c50 = Color(0xFFF0FDFA)
        val c100 = Color(0xFFCCFBF1)
        val c200 = Color(0xFF99F6E4)
        val c300 = Color(0xFF5EEAD4)
        val c400 = Color(0xFF2DD4BF)
        val c500 = Color(0xFF14B8A6)
        val c600 = Color(0xFF0D9488)
        val c700 = Color(0xFF0F766E)
        val c800 = Color(0xFF115E59)
        val c900 = Color(0xFF134E4A)
        val c950 = Color(0xFF042F2E)
    }

    object Cyan {
        val c50 = Color(0xFFECFEFF)
        val c100 = Color(0xFFCFFAFE)
        val c200 = Color(0xFFA5F3FC)
        val c300 = Color(0xFF67E8F9)
        val c400 = Color(0xFF22D3EE)
        val c500 = Color(0xFF06B6D4)
        val c600 = Color(0xFF0891B2)
        val c700 = Color(0xFF0E7490)
        val c800 = Color(0xFF155E75)
        val c900 = Color(0xFF164E63)
        val c950 = Color(0xFF083344)
    }

    object Sky {
        val c50 = Color(0xFFF0F9FF)
        val c100 = Color(0xFFE0F2FE)
        val c200 = Color(0xFFBAE6FD)
        val c300 = Color(0xFF7DD3FC)
        val c400 = Color(0xFF38BDF8)
        val c500 = Color(0xFF0EA5E9)
        val c600 = Color(0xFF0284C7)
        val c700 = Color(0xFF0369A1)
        val c800 = Color(0xFF075985)
        val c900 = Color(0xFF0C4A6E)
        val c950 = Color(0xFF082F49)
    }

    object Blue {
        val c50 = Color(0xFFEFF6FF)
        val c100 = Color(0xFFDBEAFE)
        val c200 = Color(0xFFBFDBFE)
        val c300 = Color(0xFF93C5FD)
        val c400 = Color(0xFF60A5FA)
        val c500 = Color(0xFF3B82F6)
        val c600 = Color(0xFF2563EB)
        val c700 = Color(0xFF1D4ED8)
        val c800 = Color(0xFF1E40AF)
        val c900 = Color(0xFF1E3A8A)
        val c950 = Color(0xFF172554)
    }

    object Indigo {
        val c50 = Color(0xFFEEF2FF)
        val c100 = Color(0xFFE0E7FF)
        val c200 = Color(0xFFC7D2FE)
        val c300 = Color(0xFFA5B4FC)
        val c400 = Color(0xFF818CF8)
        val c500 = Color(0xFF6366F1)
        val c600 = Color(0xFF4F46E5)
        val c700 = Color(0xFF4338CA)
        val c800 = Color(0xFF3730A3)
        val c900 = Color(0xFF312E81)
        val c950 = Color(0xFF1E1B4B)
    }

    object Violet {
        val c50 = Color(0xFFF5F3FF)
        val c100 = Color(0xFFEDE9FE)
        val c200 = Color(0xFFDDD6FE)
        val c300 = Color(0xFFC4B5FD)
        val c400 = Color(0xFFA78BFA)
        val c500 = Color(0xFF8B5CF6)
        val c600 = Color(0xFF7C3AED)
        val c700 = Color(0xFF6D28D9)
        val c800 = Color(0xFF5B21B6)
        val c900 = Color(0xFF4C1D95)
        val c950 = Color(0xFF2E1065)
    }

    object Purple {
        val c50 = Color(0xFFFAF5FF)
        val c100 = Color(0xFFF3E8FF)
        val c200 = Color(0xFFE9D5FF)
        val c300 = Color(0xFFD8B4FE)
        val c400 = Color(0xFFC084FC)
        val c500 = Color(0xFFA855F7)
        val c600 = Color(0xFF9333EA)
        val c700 = Color(0xFF7E22CE)
        val c800 = Color(0xFF6B21A8)
        val c900 = Color(0xFF581C87)
        val c950 = Color(0xFF3B0764)
    }

    object Fuchsia {
        val c50 = Color(0xFFFDF4FF)
        val c100 = Color(0xFFFAE8FF)
        val c200 = Color(0xFFF5D0FE)
        val c300 = Color(0xFFF0ABFC)
        val c400 = Color(0xFFE879F9)
        val c500 = Color(0xFFD946EF)
        val c600 = Color(0xFFC026D3)
        val c700 = Color(0xFFA21CAF)
        val c800 = Color(0xFF86198F)
        val c900 = Color(0xFF701A75)
        val c950 = Color(0xFF4A044E)
    }

    object Pink {
        val c50 = Color(0xFFFDF2F8)
        val c100 = Color(0xFFFCE7F3)
        val c200 = Color(0xFFFBCFE8)
        val c300 = Color(0xFFF9A8D4)
        val c400 = Color(0xFFF472B6)
        val c500 = Color(0xFFEC4899)
        val c600 = Color(0xFFDB2777)
        val c700 = Color(0xFFBE185D)
        val c800 = Color(0xFF9D174D)
        val c900 = Color(0xFF831843)
        val c950 = Color(0xFF500724)
    }

    object Rose {
        val c50 = Color(0xFFFFF1F2)
        val c100 = Color(0xFFFFE4E6)
        val c200 = Color(0xFFFECDD3)
        val c300 = Color(0xFFFDA4AF)
        val c400 = Color(0xFFFB7185)
        val c500 = Color(0xFFF43F5E)
        val c600 = Color(0xFFE11D48)
        val c700 = Color(0xFFBE123C)
        val c800 = Color(0xFF9F1239)
        val c900 = Color(0xFF881337)
        val c950 = Color(0xFF4C0519)
    }

    object Slate {
        val c50 = Color(0xFFF8FAFC)
        val c100 = Color(0xFFF1F5F9)
        val c200 = Color(0xFFE2E8F0)
        val c300 = Color(0xFFCBD5E1)
        val c400 = Color(0xFF94A3B8)
        val c500 = Color(0xFF64748B)
        val c600 = Color(0xFF475569)
        val c700 = Color(0xFF334155)
        val c800 = Color(0xFF1E293B)
        val c900 = Color(0xFF0F172A)
        val c950 = Color(0xFF020617)
    }

    object Gray {
        val c50 = Color(0xFFF9FAFB)
        val c100 = Color(0xFFF3F4F6)
        val c200 = Color(0xFFE5E7EB)
        val c300 = Color(0xFFD1D5DB)
        val c400 = Color(0xFF9CA3AF)
        val c500 = Color(0xFF6B7280)
        val c600 = Color(0xFF4B5563)
        val c700 = Color(0xFF374151)
        val c800 = Color(0xFF1F2937)
        val c900 = Color(0xFF111827)
        val c950 = Color(0xFF030712)
    }

    object Zinc {
        val c50 = Color(0xFFFAFAFA)
        val c100 = Color(0xFFF4F4F5)
        val c200 = Color(0xFFE4E4E7)
        val c300 = Color(0xFFD4D4D8)
        val c400 = Color(0xFFA1A1AA)
        val c500 = Color(0xFF71717A)
        val c600 = Color(0xFF52525B)
        val c700 = Color(0xFF3F3F46)
        val c800 = Color(0xFF27272A)
        val c900 = Color(0xFF18181B)
        val c950 = Color(0xFF09090B)
    }

    object Neutral {
        val c50 = Color(0xFFFAFAFA)
        val c100 = Color(0xFFF5F5F5)
        val c200 = Color(0xFFE5E5E5)
        val c300 = Color(0xFFD4D4D4)
        val c400 = Color(0xFFA3A3A3)
        val c500 = Color(0xFF737373)
        val c600 = Color(0xFF525252)
        val c700 = Color(0xFF404040)
        val c800 = Color(0xFF262626)
        val c900 = Color(0xFF171717)
        val c950 = Color(0xFF0A0A0A)
    }

    object Stone {
        val c50 = Color(0xFFFAFAF9)
        val c100 = Color(0xFFF5F5F4)
        val c200 = Color(0xFFE7E5E4)
        val c300 = Color(0xFFD6D3D1)
        val c400 = Color(0xFFA8A29E)
        val c500 = Color(0xFF78716C)
        val c600 = Color(0xFF57534E)
        val c700 = Color(0xFF44403C)
        val c800 = Color(0xFF292524)
        val c900 = Color(0xFF1C1917)
        val c950 = Color(0xFF0C0A09)
    }

    object Taupe {
        val c50 = Color(0xFFF5F4F2)
        val c100 = Color(0xFFEBE8E4)
        val c200 = Color(0xFFD6D1C9)
        val c300 = Color(0xFFBFB7AC)
        val c400 = Color(0xFFA89D8F)
        val c500 = Color(0xFF948879)
        val c600 = Color(0xFF7D7265)
        val c700 = Color(0xFF675E53)
        val c800 = Color(0xFF524B42)
        val c900 = Color(0xFF3E3933)
        val c950 = Color(0xFF1F1C18)
    }

    object Mauve {
        val c50 = Color(0xFFF8F6FA)
        val c100 = Color(0xFFF0ECF5)
        val c200 = Color(0xFFE0D9EB)
        val c300 = Color(0xFFCEC3DF)
        val c400 = Color(0xFFB8A8D1)
        val c500 = Color(0xFFA38FC3)
        val c600 = Color(0xFF8D78B0)
        val c700 = Color(0xFF75629A)
        val c800 = Color(0xFF5F4E80)
        val c900 = Color(0xFF4B3D66)
        val c950 = Color(0xFF2A2240)
    }

    object Mist {
        val c50 = Color(0xFFF4F6F9)
        val c100 = Color(0xFFE9ECF2)
        val c200 = Color(0xFFD3D9E5)
        val c300 = Color(0xFFB8C1D5)
        val c400 = Color(0xFF9AA7C0)
        val c500 = Color(0xFF808FAD)
        val c600 = Color(0xFF6A7A99)
        val c700 = Color(0xFF566582)
        val c800 = Color(0xFF44516A)
        val c900 = Color(0xFF363F54)
        val c950 = Color(0xFF1E2435)
    }

    object Olive {
        val c50 = Color(0xFFF7F8F2)
        val c100 = Color(0xFFEEF0E3)
        val c200 = Color(0xFFDDE2C7)
        val c300 = Color(0xFFC8D0A5)
        val c400 = Color(0xFFB1BC81)
        val c500 = Color(0xFF9DA964)
        val c600 = Color(0xFF84914F)
        val c700 = Color(0xFF697340)
        val c800 = Color(0xFF545C35)
        val c900 = Color(0xFF444B2C)
        val c950 = Color(0xFF25291A)
    }

    object Success {
        val c50 = Color(0xFFF0FDF4)
        val c500 = Color(0xFF22C55E)
        val c700 = Color(0xFF15803D)
    }

    object Warning {
        val c50 = Color(0xFFFFFBEB)
        val c500 = Color(0xFFF59E0B)
        val c700 = Color(0xFFB45309)
    }

    object Error {
        val c50 = Color(0xFFFEF2F2)
        val c500 = Color(0xFFEF4444)
        val c700 = Color(0xFFB91C1C)
    }

    object Info {
        val c50 = Color(0xFFEFF6FF)
        val c500 = Color(0xFF3B82F6)
        val c700 = Color(0xFF1D4ED8)
    }

    /** Every scale in declaration order, for the Colors showcase page. */
    val scales: List<Pair<String, List<Pair<Int, Color>>>> =
        listOf(
            "primary" to
                listOf(
                    50 to Primary.c50,
                    100 to Primary.c100,
                    200 to Primary.c200,
                    300 to Primary.c300,
                    400 to Primary.c400,
                    500 to Primary.c500,
                    600 to Primary.c600,
                    700 to Primary.c700,
                    800 to Primary.c800,
                    900 to Primary.c900,
                ),
            "red" to
                listOf(
                    50 to Red.c50,
                    100 to Red.c100,
                    200 to Red.c200,
                    300 to Red.c300,
                    400 to Red.c400,
                    500 to Red.c500,
                    600 to Red.c600,
                    700 to Red.c700,
                    800 to Red.c800,
                    900 to Red.c900,
                    950 to Red.c950,
                ),
            "orange" to
                listOf(
                    50 to Orange.c50,
                    100 to Orange.c100,
                    200 to Orange.c200,
                    300 to Orange.c300,
                    400 to Orange.c400,
                    500 to Orange.c500,
                    600 to Orange.c600,
                    700 to Orange.c700,
                    800 to Orange.c800,
                    900 to Orange.c900,
                    950 to Orange.c950,
                ),
            "amber" to
                listOf(
                    50 to Amber.c50,
                    100 to Amber.c100,
                    200 to Amber.c200,
                    300 to Amber.c300,
                    400 to Amber.c400,
                    500 to Amber.c500,
                    600 to Amber.c600,
                    700 to Amber.c700,
                    800 to Amber.c800,
                    900 to Amber.c900,
                    950 to Amber.c950,
                ),
            "yellow" to
                listOf(
                    50 to Yellow.c50,
                    100 to Yellow.c100,
                    200 to Yellow.c200,
                    300 to Yellow.c300,
                    400 to Yellow.c400,
                    500 to Yellow.c500,
                    600 to Yellow.c600,
                    700 to Yellow.c700,
                    800 to Yellow.c800,
                    900 to Yellow.c900,
                    950 to Yellow.c950,
                ),
            "lime" to
                listOf(
                    50 to Lime.c50,
                    100 to Lime.c100,
                    200 to Lime.c200,
                    300 to Lime.c300,
                    400 to Lime.c400,
                    500 to Lime.c500,
                    600 to Lime.c600,
                    700 to Lime.c700,
                    800 to Lime.c800,
                    900 to Lime.c900,
                    950 to Lime.c950,
                ),
            "green" to
                listOf(
                    50 to Green.c50,
                    100 to Green.c100,
                    200 to Green.c200,
                    300 to Green.c300,
                    400 to Green.c400,
                    500 to Green.c500,
                    600 to Green.c600,
                    700 to Green.c700,
                    800 to Green.c800,
                    900 to Green.c900,
                    950 to Green.c950,
                ),
            "emerald" to
                listOf(
                    50 to Emerald.c50,
                    100 to Emerald.c100,
                    200 to Emerald.c200,
                    300 to Emerald.c300,
                    400 to Emerald.c400,
                    500 to Emerald.c500,
                    600 to Emerald.c600,
                    700 to Emerald.c700,
                    800 to Emerald.c800,
                    900 to Emerald.c900,
                    950 to Emerald.c950,
                ),
            "teal" to
                listOf(
                    50 to Teal.c50,
                    100 to Teal.c100,
                    200 to Teal.c200,
                    300 to Teal.c300,
                    400 to Teal.c400,
                    500 to Teal.c500,
                    600 to Teal.c600,
                    700 to Teal.c700,
                    800 to Teal.c800,
                    900 to Teal.c900,
                    950 to Teal.c950,
                ),
            "cyan" to
                listOf(
                    50 to Cyan.c50,
                    100 to Cyan.c100,
                    200 to Cyan.c200,
                    300 to Cyan.c300,
                    400 to Cyan.c400,
                    500 to Cyan.c500,
                    600 to Cyan.c600,
                    700 to Cyan.c700,
                    800 to Cyan.c800,
                    900 to Cyan.c900,
                    950 to Cyan.c950,
                ),
            "sky" to
                listOf(
                    50 to Sky.c50,
                    100 to Sky.c100,
                    200 to Sky.c200,
                    300 to Sky.c300,
                    400 to Sky.c400,
                    500 to Sky.c500,
                    600 to Sky.c600,
                    700 to Sky.c700,
                    800 to Sky.c800,
                    900 to Sky.c900,
                    950 to Sky.c950,
                ),
            "blue" to
                listOf(
                    50 to Blue.c50,
                    100 to Blue.c100,
                    200 to Blue.c200,
                    300 to Blue.c300,
                    400 to Blue.c400,
                    500 to Blue.c500,
                    600 to Blue.c600,
                    700 to Blue.c700,
                    800 to Blue.c800,
                    900 to Blue.c900,
                    950 to Blue.c950,
                ),
            "indigo" to
                listOf(
                    50 to Indigo.c50,
                    100 to Indigo.c100,
                    200 to Indigo.c200,
                    300 to Indigo.c300,
                    400 to Indigo.c400,
                    500 to Indigo.c500,
                    600 to Indigo.c600,
                    700 to Indigo.c700,
                    800 to Indigo.c800,
                    900 to Indigo.c900,
                    950 to Indigo.c950,
                ),
            "violet" to
                listOf(
                    50 to Violet.c50,
                    100 to Violet.c100,
                    200 to Violet.c200,
                    300 to Violet.c300,
                    400 to Violet.c400,
                    500 to Violet.c500,
                    600 to Violet.c600,
                    700 to Violet.c700,
                    800 to Violet.c800,
                    900 to Violet.c900,
                    950 to Violet.c950,
                ),
            "purple" to
                listOf(
                    50 to Purple.c50,
                    100 to Purple.c100,
                    200 to Purple.c200,
                    300 to Purple.c300,
                    400 to Purple.c400,
                    500 to Purple.c500,
                    600 to Purple.c600,
                    700 to Purple.c700,
                    800 to Purple.c800,
                    900 to Purple.c900,
                    950 to Purple.c950,
                ),
            "fuchsia" to
                listOf(
                    50 to Fuchsia.c50,
                    100 to Fuchsia.c100,
                    200 to Fuchsia.c200,
                    300 to Fuchsia.c300,
                    400 to Fuchsia.c400,
                    500 to Fuchsia.c500,
                    600 to Fuchsia.c600,
                    700 to Fuchsia.c700,
                    800 to Fuchsia.c800,
                    900 to Fuchsia.c900,
                    950 to Fuchsia.c950,
                ),
            "pink" to
                listOf(
                    50 to Pink.c50,
                    100 to Pink.c100,
                    200 to Pink.c200,
                    300 to Pink.c300,
                    400 to Pink.c400,
                    500 to Pink.c500,
                    600 to Pink.c600,
                    700 to Pink.c700,
                    800 to Pink.c800,
                    900 to Pink.c900,
                    950 to Pink.c950,
                ),
            "rose" to
                listOf(
                    50 to Rose.c50,
                    100 to Rose.c100,
                    200 to Rose.c200,
                    300 to Rose.c300,
                    400 to Rose.c400,
                    500 to Rose.c500,
                    600 to Rose.c600,
                    700 to Rose.c700,
                    800 to Rose.c800,
                    900 to Rose.c900,
                    950 to Rose.c950,
                ),
            "slate" to
                listOf(
                    50 to Slate.c50,
                    100 to Slate.c100,
                    200 to Slate.c200,
                    300 to Slate.c300,
                    400 to Slate.c400,
                    500 to Slate.c500,
                    600 to Slate.c600,
                    700 to Slate.c700,
                    800 to Slate.c800,
                    900 to Slate.c900,
                    950 to Slate.c950,
                ),
            "gray" to
                listOf(
                    50 to Gray.c50,
                    100 to Gray.c100,
                    200 to Gray.c200,
                    300 to Gray.c300,
                    400 to Gray.c400,
                    500 to Gray.c500,
                    600 to Gray.c600,
                    700 to Gray.c700,
                    800 to Gray.c800,
                    900 to Gray.c900,
                    950 to Gray.c950,
                ),
            "zinc" to
                listOf(
                    50 to Zinc.c50,
                    100 to Zinc.c100,
                    200 to Zinc.c200,
                    300 to Zinc.c300,
                    400 to Zinc.c400,
                    500 to Zinc.c500,
                    600 to Zinc.c600,
                    700 to Zinc.c700,
                    800 to Zinc.c800,
                    900 to Zinc.c900,
                    950 to Zinc.c950,
                ),
            "neutral" to
                listOf(
                    50 to Neutral.c50,
                    100 to Neutral.c100,
                    200 to Neutral.c200,
                    300 to Neutral.c300,
                    400 to Neutral.c400,
                    500 to Neutral.c500,
                    600 to Neutral.c600,
                    700 to Neutral.c700,
                    800 to Neutral.c800,
                    900 to Neutral.c900,
                    950 to Neutral.c950,
                ),
            "stone" to
                listOf(
                    50 to Stone.c50,
                    100 to Stone.c100,
                    200 to Stone.c200,
                    300 to Stone.c300,
                    400 to Stone.c400,
                    500 to Stone.c500,
                    600 to Stone.c600,
                    700 to Stone.c700,
                    800 to Stone.c800,
                    900 to Stone.c900,
                    950 to Stone.c950,
                ),
            "taupe" to
                listOf(
                    50 to Taupe.c50,
                    100 to Taupe.c100,
                    200 to Taupe.c200,
                    300 to Taupe.c300,
                    400 to Taupe.c400,
                    500 to Taupe.c500,
                    600 to Taupe.c600,
                    700 to Taupe.c700,
                    800 to Taupe.c800,
                    900 to Taupe.c900,
                    950 to Taupe.c950,
                ),
            "mauve" to
                listOf(
                    50 to Mauve.c50,
                    100 to Mauve.c100,
                    200 to Mauve.c200,
                    300 to Mauve.c300,
                    400 to Mauve.c400,
                    500 to Mauve.c500,
                    600 to Mauve.c600,
                    700 to Mauve.c700,
                    800 to Mauve.c800,
                    900 to Mauve.c900,
                    950 to Mauve.c950,
                ),
            "mist" to
                listOf(
                    50 to Mist.c50,
                    100 to Mist.c100,
                    200 to Mist.c200,
                    300 to Mist.c300,
                    400 to Mist.c400,
                    500 to Mist.c500,
                    600 to Mist.c600,
                    700 to Mist.c700,
                    800 to Mist.c800,
                    900 to Mist.c900,
                    950 to Mist.c950,
                ),
            "olive" to
                listOf(
                    50 to Olive.c50,
                    100 to Olive.c100,
                    200 to Olive.c200,
                    300 to Olive.c300,
                    400 to Olive.c400,
                    500 to Olive.c500,
                    600 to Olive.c600,
                    700 to Olive.c700,
                    800 to Olive.c800,
                    900 to Olive.c900,
                    950 to Olive.c950,
                ),
            "success" to
                listOf(
                    50 to Success.c50,
                    500 to Success.c500,
                    700 to Success.c700,
                ),
            "warning" to
                listOf(
                    50 to Warning.c50,
                    500 to Warning.c500,
                    700 to Warning.c700,
                ),
            "error" to
                listOf(
                    50 to Error.c50,
                    500 to Error.c500,
                    700 to Error.c700,
                ),
            "info" to
                listOf(
                    50 to Info.c50,
                    500 to Info.c500,
                    700 to Info.c700,
                ),
        )
}
