# Kongsi brand guidelines

Colors and type of the Kongsi app. The code source of truth is `core/designsystem/.../theme/` (`Palette.kt`,
`AppColors.kt`, `AppTheme.kt`, `Type.kt`); this page explains the choices. Update both together.

## Palette: "Ink and Paper"

Kongsi is mostly a greyed espresso ink on warm paper, with one butter-yellow accent used the way you would use a
highlighter. The ink is espresso brown mixed half and half with graphite: warm enough to sit with the butter without
clashing, grey enough to read as ink rather than as a brown brand color. Amounts and names are the content; the colors
stay out of their way. The palette stays clear of the colors people link to banks (blue), e-wallets (purple, blue) and
WhatsApp (green), which Kongsi sends bills through.

| Role | Light | Dark | Used for |
| --- | --- | --- | --- |
| Paper (background) | `#FAFAF9` Stone 50 | `#0C0A09` Stone 950 | Screens, top and bottom bars |
| Card | `#FFFFFF` | `#1C1917` Stone 900 | Grouped rows, step card |
| Ink (primary) | `#372E2B` Ink 800 | `#EAE6E4` Ink 100 | Filled buttons, the raised "New bill" button, radios, switches, links |
| On ink | `#FAFAF9` Stone 50 | `#191514` Ink 950 | Text and icons on ink |
| Highlight (primary container) | `#FEF1B8` Butter 100 | `#372E2B` Ink 800 | Hint card, active tab pill, step numbers |
| On highlight | `#47340A` Butter 900 | `#FDE38A` Butter 200 | Text and icons on a highlight |
| Snackbar (inverse surface) | `#372E2B` Ink 800, text Stone 50 | `#EAE6E4` Ink 100, text Ink 950 | Toasts; the info icon and action take butter (`#FAD04F` light, `#6B4D05` dark) |
| Muted text | `#78716C` Stone 500 | `#A8A29E` Stone 400 | Subtitles, captions |
| Border | `#E7E5E4` Stone 200 | `#292524` Stone 800 | Hairlines, card outlines |

Body text stays warm Stone (`#1C1917` / `#FAFAF9`); the ink is for actions only, so a tappable thing always reads as
ink. In dark theme a butter fill turns a murky mustard, so highlights there are ink with butter text: the yellow
stays as the accent without covering an area. Proportions: about 70% paper and cards, 20–25% ink, 5–10% butter. Never fill a large area with saturated butter
(`#E5A800`); it is a scale anchor, not a surface color.

Semantic colors are unchanged: success `#22C55E`, warning `#F59E0B`, error `#EF4444`, info `#3B82F6`. With a neutral
brand color they are easy to spot; still pair them with an icon or text, never color alone.

### Contrast (WCAG 2.1)

| Pair | Ratio |
| --- | --- |
| On ink / ink, light | 12.7:1 |
| On ink / ink, dark | 14.6:1 |
| Ink / butter card, light | 11.6:1 |
| On butter / butter, light | 10.5:1 |
| Butter text / ink highlight, dark | 10.4:1 |
| Muted text / paper, light | 4.6:1 |
| Snackbar text, light / dark | 12.7:1 / 14.6:1 |
| Snackbar butter icon or action, light / dark | 8.9:1 / 6.3:1 |

## Typography

Xmind (SIL OFL, Google Fonts) for every style, weights 400 to 800, bundled in `core/designsystem/src/main/res/font/`.
