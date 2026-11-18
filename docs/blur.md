# Blur

How the app blurs, and why. Everything is in one package, `:core:designsystem`'s
`core/designsystem/src/main/kotlin/com/bobbyesp/metadator/core/designsystem/blur/`, with its test
in `core/designsystem/src/test/kotlin/.../blur/`. The package depends on Compose, Material 3 and
[Haze](https://github.com/chrisbanes/haze) only. No other module names Haze: Haze is an
`implementation` dependency of the design system, so a module that tried would not compile.

Material has no blur tokens. Blur stands in for elevation: where Material would cast a shadow under
something that floats over content, the app blurs instead, and keeps the surface's Material color
role, only translucent.

## The calls

- **`rememberBlurSource()` and `Modifier.blurSource(source, zIndex)`** record the content that
  moves beneath a floating surface. The source is a sibling of what it blurs, never an ancestor: a
  surface inside its own source would blur itself. Content passes beneath the surface padded, not
  inset, or there is nothing there to frost. `zIndex` orders sources that overlap (the wide
  player's lyrics over its backdrop).
- **`Modifier.frosted(source, containerColor, shape, amount = { … })`** is a surface over a
  source: what is behind it, blurred, under `containerColor` made translucent. Pass the role the
  surface would have as a solid color. The surface's own container becomes transparent and loses
  its shadow. The wide player's floating controls and the menus are frosted.
- **`Modifier.blurHalo(source, shape, strength = { … })`** lifts what floats: the content around
  the element goes out of focus, most at its edge, and is sharp again `spread` away. Where a shadow
  darkens, this softens. The halo follows the element's shape, reaches a little further below it
  than above, as Material's light does, and is drawn outside the element without changing its
  layout (`reserveSpace` takes the room inside the layout instead, for a popup, whose window ends
  at its content). The player's bar, the floating toolbars and the menus have one.
- **`Modifier.outOfFocus { radius }`** blurs content itself: the screen behind a sheet or a
  dialog. Metadator's `outOfFocus(fraction, scrim)` is the same blur, taking
  `BlurDefaults.BehindOverlayRadius` by a fraction a gesture can drive, plus a scrim over it: the
  shell does it to everything behind the player as it opens, and `BehindTheGesture` to the screen
  behind a predictive back.
- **`Modifier.dissolved { presence }` and `Modifier.defocused { amount }`** are for what comes and
  goes as one surface turns into another: as faint and as blurred as it is absent, sharpening as
  it shows. The player's sheet does it to what its bar and its full screen do not share. They are
  `Modifier.blur`, not Haze, so unlike a frosted surface they can run during a shared element
  transition. `Focus.kt` holds these and `outOfFocus(fraction, scrim)`: they are Metadator's, and
  not in Sona's package.

Callers never see a Haze type. `BlurDefaults` holds the radii, spreads and opacities.

## The level

`BlurLevel` is `Off`, `Reduced` or `Full`, for the whole app. The theme provides it once, with
`ProvideBlurLevel(rememberBlurLevel(…))` around `MaterialExpressiveTheme`, and every modifier falls
back on its own, so a screen never checks the device:

| Level | When | What blurs |
|---|---|---|
| `Off` | below Android 12, Android Go, battery saver, a hot phone (thermal *severe*) | nothing: a frosted surface is its container color at 96 %, a halo and out-of-focus draw nothing |
| `Reduced` | under 3 GB of memory, a warm phone (thermal *moderate*), the system's window blur off | frosted surfaces only, at Haze's lowest input resolution |
| `Full` | otherwise | everything, at Haze's balanced resolution |

`rememberDeviceBlurLevel()` listens for battery saver, the thermal status and the system's window
blur setting while the theme is shown, so the level changes as the phone does.
`rememberBlurLevel(preferred)` caps it with the user's choice: less blur than the device allows,
never more. A phone in battery saver or too hot stops blurring whatever was chosen.

Where a screen has a solid alternative it asks the level or `BlurDefaults.isHaloAvailable` (`Full`,
from Android 13, where Haze can vary a blur's radius):

- An element that a halo lifts keeps its shadow where there is no halo (the player's bar, the
  floating toolbars, the menus' groups).
- Content out of focus stays sharp below `Full`, so it has to be set apart another way: the
  shell's scrim is darker there (`BlurDefaults.SharpScrimOpacity`), and the wide player's backdrop,
  a cover blurred and stretched over the screen, is a gradient of the theme's colors instead.
- `LocalBackdropSource` is what the shell records of every screen for what floats over all of them:
  the player's bar, which is its sibling there, and the menus, which are windows of their own. It
  is `null` where nothing is recorded (a preview, an overlay in a window of its own), and a menu
  keeps Material's own there. A screen with a toolbar of its own records its lists itself, since the
  toolbar is inside what the shell records.

## Smooth by construction

What animates is a lambda (`amount`, `strength`, `radius`, `fraction`), read while drawing, so an
animation redraws and never recomposes. Haze builds its blur again for a new color or radius, and
only fades it for a new alpha, so a frost animates its `amount`, never its color, and a halo's
radius moves in whole dps (a dozen rebuilds per fade, not one per frame). A source blurred by two
surfaces renders once for both.

- A halo that is not showing is taken off the chain rather than left at strength zero, because a
  source records its content only while something blurs it. Whether it is on the chain is decided
  by a `derivedStateOf`, which changes twice per fade; its strength is read from the animation's
  `State` inside the lambda.
- **Out of the element's own enter and exit animation**, which would scale, slide and clip the halo
  with the element. Animate its `strength` instead.
- **Not while a shared element transition runs.** What travels is drawn in the transition's
  overlay, where a frosted surface flickers: it is its solid color until the transition settles.
- A surface whose frost has to appear or disappear draws its resting color behind
  (`drawBehind { drawRect(surface) }`) and animates `amount`, never the color.
