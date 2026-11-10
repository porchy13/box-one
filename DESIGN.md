# DESIGN.md

Design guide of Box One, a desktop application (Java / JavaFX).
This document is the reference for every visual or interaction decision. When in doubt, the rule is: **look and behave as close as possible to a native application of the system.** JavaFX draws its own controls (Modena), it cannot use those of the system; what it can take from the system, it takes: menus, shortcuts, order of the buttons, font, light or dark mode, and the colours — accent, background, text.

> The examples of this guide come from a sample task management application. The rules apply whatever the business content; §14 describes how they apply to this project.

---

## 1. Principles

1. **Native first.** Title bar, menus, shortcuts, order of the dialog buttons, font, light/dark theme and colours (accent, background, text) come from the operating system. We do not redraw them, and the application has no colour of its own for what the system already gives.
2. **Calm.** A single accent colour, the one of the system, used sparingly: primary action, selection, focus. Everything else is neutral. If two elements compete for attention, one of them is wrong.
3. **Content before frame.** Separations are made with space and thin borders, not with stacked shadowed cards. No decorative gradients, no glass effects.
4. **Dense but readable.** It is a working tool: rows of 44 px, text of 13 px, a clear hierarchy. One must see a lot without squinting.
5. **Everything from the keyboard.** Every action reachable with the mouse is reachable with the keyboard too, and the focus is always visible.

---

## 2. Integration with the operating system

| Topic | Rule | JavaFX implementation |
|---|---|---|
| Window | Native decoration, never a custom title bar | `StageStyle.DECORATED` (default) |
| Menu bar | Global menu on macOS, inside the window on Windows/Linux | `menuBar.setUseSystemMenuBar(true)` |
| Shortcuts | Ctrl on Windows/Linux, ⌘ on macOS | Always `KeyCombination.valueOf("Shortcut+N")`, never a hard-coded `Ctrl+` |
| Mnemonics | Underlined letters (`_File`) on Windows/Linux | `mnemonicParsing` enabled on menus and buttons |
| Dialog buttons | Order of each OS (OK at the right on macOS, etc.) | Use `ButtonBar` / `Dialog` with `ButtonData`, never order by hand |
| Theme | Follow the light/dark mode of the system | `Platform.getPreferences().colorSchemeProperty()` (JavaFX 22+) |
| Accent | Accent of the system, always; followed when the user changes it | `Platform.getPreferences().accentColorProperty()` |
| Background and text | Those of the system, the neutral shades derived from them (§3); followed when they change | `backgroundColorProperty()`, `foregroundColorProperty()` |
| Font | System font | `-fx-font-family: "System"` |
| Text size | Respect the scale factor of the OS | Sizes in `em` in the CSS, never fixed pixels on `.root` |
| Preferences | "Preferences…" item in the application menu on macOS, "Options" in Edit/Tools elsewhere | Build the menu according to `System.getProperty("os.name")` |
| Persistence | Size, position, maximized state and width of the panels restored at start-up | `java.util.prefs.Preferences` |

Minimum size of the main window: **960 × 600**. Default size at the first start: **1280 × 800**, centred.

---

## 3. Colours

All the colours are *looked-up colors* (tokens `-c-…`) defined on `.root`. Most of them come from the **operating system**: its accent, its background and its text, read by JavaFX (`Platform.getPreferences()`, §2) and followed when the user changes them; the neutral shades and the tints of the accent are derived from them. Only the colours the system does not give (errors, warnings, success, shadow) are chosen by the application, in the style sheets of the tokens. No hexadecimal code anywhere else in the CSS or in the Java code.

### Tokens from the system

Each derived token mixes a colour of the system into its background, in the sRGB space, at a fixed share: the same rule gives the light and the dark shades, whatever the system and its theme.

| Token | Source | Use |
|---|---|---|
| `-c-surface` | Background of the system | Main content area, fields, toolbars |
| `-c-bg` | Background + 4 % of the text | Background of the window, detail panel |
| `-c-sidebar` | Background + 7 % of the text | Side navigation bar |
| `-c-border` | Background + 14 % of the text | Separators, light borders |
| `-c-border-strong` | Background + 24 % of the text | Borders of fields and buttons |
| `-c-text` | Text of the system | Main text |
| `-c-text-2` | Background + 65 % of the text | Secondary text, labels |
| `-c-text-3` | Background + 50 % of the text | Tertiary text, placeholders, counters |
| `-c-accent` | Accent of the system | Primary button, checked boxes, focus |
| `-c-accent-subtle` | Background + 12 % of the accent | Focus halo |
| `-c-selection` | Background + 20 % of the accent | Selected row or element |
| `-c-on-accent` | The lighter of the background and the text, the darker one if the lighter is below 3:1 on the accent | Text on an accent background |

For example, with the default colours of macOS in light mode (accent `#007AFF`, background `#FFFFFF`, text `#000000`): `-c-bg` `#F5F5F5`, `-c-border` `#DBDBDB`, `-c-text-2` `#595959`, `-c-selection` `#CCE4FF`, `-c-on-accent` `#FFFFFF`.

### Tokens of the application

| Token | Light | Dark | Use |
|---|---|---|---|
| `-c-danger` | `#B4363A` | `#E0787B` | Errors, delays, destructive actions |
| `-c-warning` | `#A86A12` | `#D9A24F` | Warnings |
| `-c-success` | `#2E7D4F` | `#6CC08B` | Something done, e.g. the tick of a result entered |
| `-c-shadow` | `rgba(0, 0, 0, 0.16)` | `rgba(0, 0, 0, 0.40)` | The single shadow (§5) |

### Rules of use

- The accent is the one the user chose for the system: the application looks like the other applications of the desktop. It is never replaced by a colour of the application.
- Red is used only for what requires an action (delay, error, deletion). Never as a decoration.
- Minimum contrast: 4.5:1 for the text (`-c-text-2` included, which its share guarantees on the backgrounds of the systems), 3:1 for the borders of the controls. `-c-text-3` is reserved for non-essential information. The text on the accent follows the systems, which put white on their accent even below 4.5:1 (4.0:1 on the blue of macOS); a light accent (yellow…) gets the dark text.
- Colour never carries an information alone: an overdue task has a date in red **and** appears in the "Overdue" section.
- Project colours (dots): `#2D6A57`, `#5A6FB0`, `#A86A12`, `#8A5A9E`, `#4F7F95`, `#9A5B4A`. They only appear as dots of 8 px, never as a background.

---

## 4. Typography

Font: **system font** (Segoe UI Variable on Windows 11, SF Pro on macOS, the interface font of the distribution on Linux). A single family, no embedded font for the interface.

| Role | Size | Weight | Example |
|---|---|---|---|
| View title | 1.54em (≈ 20 px) | Semi-bold | "Kitchen renovation" |
| Panel title | 1.15em (≈ 15 px) | Semi-bold | Title of the task in the detail panel |
| Body | 1em (13 px) | Normal | List rows, fields, buttons |
| Section header | 0.92em (≈ 12 px) | Semi-bold, `-c-text-2` | "This week" |
| Label | 0.92em | Normal, `-c-text-2` | "Due date" |
| Caption | 0.85em (≈ 11 px) | Normal, `-c-text-3` | Counters, status bar |

Rules:

- Sentence case everywhere: "New task", not "New Task" nor "NEW TASK". No labels in capitals.
- Two weights only: normal and semi-bold. Bold serves the hierarchy, not the emphasis.
- Tabular figures for the dates and the numeric columns (alignment).
- Line length of free text areas: 80 characters at most.
- Line spacing of multi-line text areas: 1.5.

---

## 5. Spacing, dimensions, shapes

Base grid of **4 px**. Allowed values: 4, 8, 12, 16, 20, 24, 32.

| Element | Value |
|---|---|
| Height of a button / field (toolbar) | 30 px |
| Height of a field (form) | 28 px |
| Height of a list row | 44 px |
| Height of a navigation item | 30 px |
| Toolbar | 48 px |
| Status bar | 24 px |
| Inner margin of the panels | 20 px |
| Left margin of the list content | 24 px |
| Width of the side bar | 224 px (resizable, 180–320) |
| Width of the detail panel | 340 px (resizable, 280–480, collapsible) |

Radii:

| Element | Radius |
|---|---|
| Buttons, fields, selected rows, navigation items | 4 px |
| Check boxes | 3 px |
| Context menus, popovers, tooltips | 6 px |
| Dialogs (if not native) | 8 px |
| Tags | fully rounded |

Shadows: only for what floats above the content (menus, popovers, drop-down lists). A single shadow:
`dropshadow(gaussian, rgba(0,0,0,0.16), 12, 0, 0, 4)`. No shadow on the elements laid in the window.

---

## 6. Structure of the window

```
┌──────────────────────────────────────────────────────────────┐
│ Native title bar                                             │
├──────────────────────────────────────────────────────────────┤
│ Menus (Windows/Linux only)                                   │
├──────────────────────────────────────────────────────────────┤
│ [+ Primary action] │ [tools]              [Search    Ctrl+F] │
├────────────┬──────────────────────────────┬──────────────────┤
│ Navigation │ View title        counter    │ Detail of the    │
│            │ Section                      │ selected         │
│ Views      │ ☐ Element ............ date   │ element          │
│            │ ☐ Element ............ date   │                  │
│ Groups     │                              │                  │
│            │                              │  [Sec.] [Prim.]  │
├────────────┴──────────────────────────────┴──────────────────┤
│ Save state · selection                        synchronisation│
└──────────────────────────────────────────────────────────────┘
```

- Three-panel layout through `SplitPane`, positions of the dividers remembered.
- The content is aligned to the left. Only the dates and the numbers are aligned to the right.
- The detail panel collapses automatically below 1100 px of width, and then opens as an overlay.
- The toolbar holds **one** primary action (filled accent button), at the left. The other tools are icon buttons with a tooltip.
- The search is at the right of the toolbar, with its shortcut shown.

---

## 7. Components

### Buttons

| Variant | Appearance | Use |
|---|---|---|
| Primary | Background `-c-accent`, text `-c-on-accent` | Once per zone (toolbar, panel, dialog) |
| Secondary | Background `-c-surface`, border `-c-border-strong` | Common actions |
| Icon | Secondary, square of 30 × 30, icon of 16 px | Tools of the toolbar, always with a tooltip |
| Destructive | Secondary, text `-c-danger` on hover and in the confirmation dialogs | Delete |

The label says exactly what happens: "Mark as done", not "OK"; "Delete the task", not "Confirm".

### Fields

- Border `-c-border-strong`, background `-c-surface`, radius 4 px.
- Focus: border `-c-accent` + halo of 2 px `-c-accent-subtle`. This focus style is the same for every control.
- Error: border `-c-danger` and a message under the field ("The date must be after today"), never only a colour.
- Placeholder in `-c-text-3`, never used instead of a label.

### Lists

- Rows of 44 px, separated by a hairline `-c-border`, without stripes.
- Hover: background `-c-bg`. Selection: background `-c-selection`, radius 4 px, hairlines around hidden.
- Multiple selection: Shift and Shortcut, as in the file explorer.
- A done element turns `-c-text-3` struck through, and moves down into its section after 2 s (to leave time to undo).
- The sections ("Overdue", "This week") are simple text headers, not cards.

### Side navigation

- Items of 30 px, icon of 16 px at the left, counter as a caption at the right.
- Active item: background `-c-selection`, semi-bold text.
- The groups ("Projects") have a semi-bold caption header, `-c-text-3`.

### Tags

Background `-c-bg`, border `-c-border`, caption text `-c-text-2`. Neutral: colour is reserved for the project dots.

### Dialogs

- Always a JavaFX `Dialog` / `Alert` with a `ButtonBar`, for the native order of the buttons.
- Modal only if a decision is required. Otherwise, prefer an action that can be undone.
- Title = the question or the action ("Delete “Fit the splashback”?"), text = the consequence ("The task and its 3 subtasks will be deleted.").

### Status feedback

- Status bar at the bottom: save state, number of selected elements, synchronisation.
- After a destructive or bulk action: a discreet notification at the bottom of the list with an "Undo" button, visible for 6 s.
- Empty states: a sentence that says what to do ("No task in this project. Press Ctrl+N to create one.") and the matching button. No illustration.
- Errors: say what happened and how to correct it ("Cannot save: the disk is full. Free some space then try again."). No apologies, no vague message.

---

## 8. Icons

- Line style, stroke of 1.6 px at 16 px, round ends, a single icon family in the whole application (Ikonli with a consistent line pack, or home-made `SVGPath`).
- Size: 16 px in the interface, 20 px at most in the empty states.
- Colour: `-c-text` by default, `-c-accent` for the active element only; in a list, the warning icon in `-c-warning`, the tick of a row that is done in `-c-success` (warning first: a row to check never shows the tick).
- Icons are never alone without a label or a tooltip.
- Bitmap images: provide the `@2x` variants (JavaFX loads them automatically on HiDPI screens).

---

## 9. Motion

Animations answer an action of the user, they do not decorate.

| Situation | Animation | Duration |
|---|---|---|
| Opening/closing of the detail panel | Horizontal slide | 180 ms |
| Change of selection | None (immediate) | — |
| Done element changing section | Fade + move | 200 ms, after 2 s |
| Appearance of a notification | Fade | 120 ms |
| Hover | No transition | — |

Curve: `Interpolator.SPLINE(0.2, 0, 0, 1)`. A "Reduce animations" option in the preferences replaces every animation by an immediate change.

---

## 10. Keyboard and accessibility

Minimum shortcuts (written here for Windows/Linux, Ctrl becomes ⌘ on macOS):

| Action | Shortcut |
|---|---|
| New element | Ctrl+N |
| Search | Ctrl+F |
| Open the detail / edit | Enter |
| Mark as done | Ctrl+Enter |
| Delete | Del (⌫ on macOS) |
| Undo / Redo | Ctrl+Z / Ctrl+Shift+Z (and Ctrl+Y on Windows) |
| Toggle the side bar | Ctrl+B |
| Preferences | Ctrl+, |
| Move from a panel to another | F6 / Shift+F6 |

Accessibility:

- Logical tab order: toolbar → navigation → list → detail.
- Focus always visible, in the lists too (ring `-c-accent` on the cell).
- `setAccessibleText()` on every icon button and colour dot.
- Minimum clickable area of 24 × 24 px, even when the icon is 16 px.
- Test with Narrator (Windows) and VoiceOver (macOS), and with a system zoom of 150 %.

---

## 11. Writing

- Vocabulary of the user, not of the code: "Projects", not "Collections"; "Due date", not "dueDate".
- An action keeps the same name everywhere: the button "Mark as done" produces the notification "Task done".
- Verbs on the buttons, short sentences, sentence case.
- Relative dates when clearer ("Today", "Mon. 5 Oct."), absolute in the detail panel. Format according to the `Locale` of the system.

---

## 12. JavaFX implementation

### Organisation of the style sheets

```
src/main/resources/css/
  tokens-light.css    # colour tokens of the light theme that do not come from the system
  tokens-dark.css     # the same, for the dark theme
  base.css            # typography, spacing, controls
  components.css      # navigation, lists, detail, status bar
```

We start from Modena (the default theme of JavaFX) and override it through its own variables, to keep the native behaviour of the controls. The tokens from the system are not in a file: they are computed from the colours of the system and applied as a style sheet built in memory (`data:text/css;base64,…`), before the others.

### Tokens and connection to Modena

```css
/* Built in memory from the colours of the system (here the default ones of macOS, light mode). */
.root {
    -c-bg: #F5F5F5;
    -c-surface: #FFFFFF;
    -c-sidebar: #EDEDED;
    -c-border: #DBDBDB;
    -c-border-strong: #C2C2C2;
    -c-text: #000000;
    -c-text-2: #595959;
    -c-text-3: #808080;
    -c-accent: #007AFF;
    -c-accent-subtle: #E0EFFF;
    -c-selection: #CCE4FF;
    -c-on-accent: #FFFFFF;
}
```

```css
/* tokens-light.css */
.root {
    -c-danger: #B4363A;
    -c-warning: #A86A12;
    -c-success: #2E7D4F;
    -c-shadow: rgba(0, 0, 0, 0.16);
}
```

```css
/* base.css */
.root {
    -fx-font-family: "System";
    -fx-font-size: 1em;

    /* Connection of the tokens to Modena */
    -fx-base: -c-surface;
    -fx-background: -c-bg;
    -fx-control-inner-background: -c-surface;
    -fx-accent: -c-accent;
    -fx-default-button: -c-accent;
    -fx-focus-color: -c-accent;
    -fx-faint-focus-color: -c-accent-subtle;
    -fx-selection-bar: -c-selection;
    -fx-selection-bar-non-focused: -c-selection;
    -fx-text-base-color: -c-text;
    -fx-text-background-color: -c-text;
    -fx-prompt-text-fill: -c-text-3;
}

.button {
    -fx-background-color: -c-border-strong, -c-surface;
    -fx-background-insets: 0, 1;
    -fx-background-radius: 4, 3;
    -fx-padding: 0 12 0 12;
    -fx-pref-height: 30;
    -fx-font-weight: 500;
}

.button:default {
    -fx-background-color: -c-accent;
    -fx-background-insets: 0;
    -fx-text-fill: -c-on-accent;
}

.text-field, .combo-box, .date-picker .text-field {
    -fx-background-color: -c-border-strong, -c-surface;
    -fx-background-insets: 0, 1;
    -fx-background-radius: 4, 3;
}

.text-field:focused {
    -fx-background-color: -c-accent-subtle, -c-accent, -c-surface;
    -fx-background-insets: -2, 0, 1;
    -fx-background-radius: 6, 4, 3;
}

.title-view   { -fx-font-size: 1.54em; -fx-font-weight: 600; }
.title-panel  { -fx-font-size: 1.15em; -fx-font-weight: 600; }
.section-head { -fx-font-size: 0.92em; -fx-font-weight: 600; -fx-text-fill: -c-text-2; }
.caption      { -fx-font-size: 0.85em; -fx-text-fill: -c-text-3; }
```

### Following the theme and the colours of the system

```java
public final class ThemeManager {

    private final Scene scene;
    private final Platform.Preferences system = Platform.getPreferences();

    public ThemeManager(Scene scene) {
        this.scene = scene;
        for (ObservableValue<?> value : List.of(system.colorSchemeProperty(), system.accentColorProperty(),
                system.backgroundColorProperty(), system.foregroundColorProperty())) {
            value.addListener((obs, old, now) -> apply());
        }
        apply();
    }

    private void apply() {
        boolean dark = system.getColorScheme() == ColorScheme.DARK;
        scene.getStylesheets().setAll(
                systemTokens(system.getAccentColor(), system.getBackgroundColor(), system.getForegroundColor()),
                css(dark ? "tokens-dark.css" : "tokens-light.css"),
                css("base.css"),
                css("components.css"));
    }

    /** The tokens of §3 mixed from the colours of the system, as a data: style sheet. */
    private static String systemTokens(Color accent, Color background, Color text) { … }

    private static String css(String name) {
        return ThemeManager.class.getResource("/css/" + name).toExternalForm();
    }
}
```

### Native behaviours not to forget

```java
// Global menu on macOS
menuBar.setUseSystemMenuBar(true);

// Cross-platform shortcuts
newItem.setAccelerator(KeyCombination.valueOf("Shortcut+N"));

// Native order of the dialog buttons
Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
alert.getButtonTypes().setAll(
        new ButtonType("Delete the task", ButtonBar.ButtonData.OK_DONE),
        new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));

// Minimum size and restoration of the window
stage.setMinWidth(960);
stage.setMinHeight(600);
```

---

## 13. Review checklist

Before merging a screen:

- [ ] No hard-coded colour outside the `tokens-*.css` files.
- [ ] Tested in light, in dark, and with several accents of the system, a light one (yellow) included.
- [ ] Tested on Windows and macOS (at least), at 100 % and 150 % scale.
- [ ] A single primary action per zone.
- [ ] Everything reachable from the keyboard, focus visible everywhere.
- [ ] The shortcuts use `Shortcut`, the dialogs use `ButtonBar`.
- [ ] Empty, loading and error states written and shown.
- [ ] Labels in sentence case, vocabulary of the user.

---

## 14. Application to this project

The rules above apply to Box One as follows. The screens arrive phase after phase (development plan); the rows
marked _proposed_ are the direction of the design, to be confirmed by the maintainer when their phase starts, then
rewritten as decisions.

| Topic | In this project |
|---|---|
| Style sheets | `ui/src/main/resources/ch/varani/boxone/ui/` (`tokens-light.css`, `tokens-dark.css`, `base.css`, `components.css`), in the package of the application so that the named module finds them; applied by `Theme`, after the tokens taken from the colours of the system (`SystemColors`, `Palette`: a style sheet built in memory). The tokens are defined on `.root` **and** `.dialog-pane`, so that the message boxes get them. The page of the map gets the values of all of them. |
| Structure of the window | Applied (phase 1): the **map** is the content of the centre (§6), the list of §6 is replaced by it. The status bar holds the state of the work (what to do next: open a map, place the start, place the finish; the failures, saying how to correct them). The distance and the duration of the route, the result of the view, are in a card at the top right of the map (`-c-surface`, border `-c-border`, radius 8 px, the single shadow), which lets the clicks through to the map: in the status bar, they were missed. _Proposed_: side navigation: Route, Roadbook, Training. Detail panel: the roadbook strip of the route (the boxes one under the other, the selected box highlighted on the map), then the editor of the selected box (phase 5). The primary action of the toolbar is the action of the view ("Generate the route", "Start the session"…). Applied (phase 3): the seed of the detour shown at the right of the status bar, after its label, in a read-only field that can be selected and copied, drawn as the text of the bar until it has the focus; the card of the route is titled "Detour" when it shows a detour. |
| Map | Applied (phase 1): Leaflet in a `WebView`, bundled (`map/`). While the graph is loaded or imported, a veil (`-c-bg`, dimmed) covers the map, which cannot be clicked, with a card in the middle: a progress indicator, what is happening and, for an import, how long it takes. The page (`map.css`, `map.js`) holds **no colour value**: `Theme` hands it the values of the tokens at load and when the theme changes; the style sheet of Leaflet, bundled unchanged, keeps its own. The attribution of the tile provider and "© OpenStreetMap contributors" stay visible in a corner of the map, in `-c-text-2` on `-c-surface`, as text (no link, which would open in the map). The fastest route is a dashed line `-c-text-3`; start and finish are discs `-c-accent` with a letter (`D` / `A` in French, `S` / `F` in English) and a tooltip, never a colour alone. Zoom with the mouse wheel and the buttons + / −, the keys + / − and the arrows when the map has the focus; the focus of the map is a ring `-c-accent`. Applied (phase 3): the detour a solid line `-c-accent` over the dashed fastest route, both shown whole. _Proposed_: zoom by Shortcut++ / Shortcut+− from the window. |
| Roadbook boxes | Phase 2: the boxes are only exported, not shown in the window. _Proposed_ (phases 2 and 4): a roadbook is a printed object, and training means reading the real thing: the boxes are drawn **as on paper** — dark ink on a light background in both themes, with two tokens of the same value in both files (`-c-paper`, `-c-ink`). One box = one row: total distance and partial distance (right-aligned, two decimals), the tulip, the notes and the heading. The distances follow the `Locale` of the application. |
| Symbols | _Proposed_ (phase 4): the catalogues hold SVG paths without colour; the rendering strokes them in `-c-ink` with the line rules of §8 adapted to the catalogue (its own thickness when the rulebook sets one). A symbol always has its meaning as accessible text (`setAccessibleText`) and as tooltip. The OpenRally export renders the tulips and the notes as PNG, black on white (Terra Pirata does not show SVG). |
| Training | _Proposed_ (phase 7): one question per screen, the answers as buttons also reachable with the keys 1 to 4; a right answer in `-c-success` **and** the word "Right", a wrong answer in `-c-danger` **and** the right answer shown (colour never alone, §3). Blind mode: the map is hidden by an empty state that says so, not only greyed. The progress uses the components of §7 (tables, captions), no gamified decoration. |
| Preferences | Applied (phase 1): a dialog of settings (`SettingsDialog`, §7) in two sections ("Background map", "Navigation on the map"), each field at the right of its label (right-aligned, `-c-text-2`), its caption under it: the provider of the background map (national maps of swisstopo, MapTiler) and the key of MapTiler in a password field, with a caption saying where to get it and where it is kept; the field is disabled for swisstopo and "Save" is disabled while MapTiler has no key; the sensitivity of the zoom to the mouse wheel and to the trackpad (low, medium — the default —, high, very high: one level every 60, 30, 15 or 8 pixels of scroll). JavaFX cannot add an item to the application menu of macOS: "Settings…" (Shortcut+,) in the File menu on macOS, "Options…" in a Tools menu elsewhere. _Proposed_: theme, reduced animations, map extracts; a manual theme opposite to the mode of the system has no colours of the system to take (JavaFX gives only those of the current mode), to be decided with its phase. |
| Persistence | Applied (phase 1): the settings in `settings.properties` of the data folder of the user (`DataFolder`) rather than `java.util.prefs`, so that the tests never touch the preferences of the user. _Proposed_: the state of the window in the same folder. |
| Menus | File menu: "Open a map…" (Shortcut+O), "Export the roadbook…" (Shortcut+E, disabled while no route is shown; a save dialog limited to the GPX files, the result or the failure in the status bar); then "Settings…" on macOS, where the application menu quits, or Quit (Shortcut+Q) elsewhere, with a Tools menu holding "Options…". Route menu (phase 3): "Generate a detour…" (Shortcut+D, disabled while no route is shown), a dialog (§7) with the budget (15 or 30 min) and the seed (a new one proposed, 1 to 6 digits; otherwise the field is marked, its caption says so and "Generate" is disabled), each with its caption; the progress and the failure in the status bar. |
| Icons | Home-made line icons (`SVGPath`), no Ikonli dependency, as soon as the first icon is needed. |
| Not applied yet | Everything except the frame of the main window (menus, map, status bar, theme of the system) and the settings of the background map: toolbar, side navigation, detail panel, other settings (manual theme), restoration of the window, icons, motion, mnemonics of the buttons, About; tabular figures (not available in the CSS of JavaFX); the contrast of 3:1 of the borders of the controls (§3: `-c-border-strong` is below it, about 1.8:1 on a white surface, as discreet as the borders of the systems and as the former palette). |
