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
| Structure of the window | Applied (phase 1): the **map** is the content of the centre (§6), the list of §6 is replaced by it. The status bar holds the state of the work (what to do next: open a map, place the start, place the finish, move them once the route is shown; the failures, saying how to correct them). The distance and the duration of the route, the result of the view, are in a card at the top right of the map (`-c-surface`, border `-c-border`, radius 8 px, the single shadow), which lets the clicks through to the map: in the status bar, they were missed. Applied (phase 4): the detail panel holds the roadbook of the route (row "Roadbook boxes"). Applied (phase 5; decision of the maintainer, 2026-11-17, the editor under the strip being too far from the tulip and too long): **three columns**, the strip of the roadbook at the left, the map in the middle, the editor of the box selected at the right (a `SplitPane`, the side columns keeping their widths when the window is resized; an empty state "Select a box in the roadbook to correct it." while none is selected), the editor on the full height: its title ("Box 12"), the diagram of the box (its tulip and its notes, with their handles, seen whatever the tab), then five **tabs**, "Box", "Tulip" (disabled at the start and at the finish), "Symbols", "Abbreviations" and "Notes" (the danger and the free text), each scrolled when it is higher than the tab; in "Symbols" and "Abbreviations", the choice of an element comes first, the elements of the box under it, an empty list saying so ("No symbol in this box."), the position of a symbol shown only while a symbol placed freely is selected (maintainer, 2026-11-17: the empty zone before empty positions was not understood). In the tab "Box", each field under its label, its caption under it giving the value computed (the panel is narrow): the total distance, the heading, the display of the heading (where the FIA asks for it, always, never); the waypoint (phase 5, increment 4: a choice, none or one of the seven kinds of the FIA, "WPM, masked"…, or, increment 5, the start or the finish of a speed zone, "DZ, start of a speed zone"…, then its radii of opening and of validation in metres, each caption giving the radius of the FIA, and the speed limit of a DZ in km/h, 10 to 130; the radii disabled without waypoint, the speed but for a DZ); then "Reset the box" and "Remove the box" (destructive, secondary: no primary action, a field is applied by Enter or when it loses the focus), without confirmation as both can be undone (§7); the tulip of the box drawn as on paper by its catalogue, a handle in `-c-accent` at the end of each branch and of the entry (a ring, filled for the branch selected; a click selects, a drag turns the branch, applied once dropped), and the same for the keyboard and the readers of the screen in the form: the branch, its angle (its bounds in the caption), its road, "Take this branch", "Remove the branch", "Add a branch", "Reset the tulip" (all undone by Undo); in the tab "Notes", each field under its label: the danger (a choice: none, "! Danger of level 1" to "!!! Danger of level 3"), the abbreviations of the box (a short list, each line ending with a cross that removes it — Del or ⌫ too —, then the abbreviation to add, chosen in a **picker**: a field of search, case and accents ignored, above the list of the lexicon, each written as the catalogue writes it with its meaning, "kpL — Keep to the left"; "Add the abbreviation", a double click or Enter adds it), the free text (a field applied by Enter or when it loses the focus, its caption giving its rule, marked when it is not English or too long), the symbols of the box (a short list, each line with the symbol drawn at the left of its name and a cross at its right, then the symbol to add in a picker, each drawn at the left of its name, and its place, before or after the junction, "Add the symbol"; a symbol beside the road has no place to choose: it is added in a free corner of the tulip, then dragged in the tulip with the mouse — the handles of the branches taken first —, or, selected in the list, its horizontal and vertical positions typed in percent from the centre, applied by Enter or when they lose the focus, marked when out of −100 to 100; the start and the finish take the symbols beside the road only, their caption saying why), a button disabled when it cannot act (the list full, nothing selected), all undone by Undo; Del (⌫ on macOS) in the strip removes the box selected too (§10). Undo and Redo of a change of the roadbook leave the map where it is, at its zoom, and the box selected selected. A value out of its rules marks its field and says why under it (§7). _Proposed_: side navigation: Route, Roadbook, Training. The primary action of the toolbar is the action of the view ("Generate the route", "Start the session"…). Applied (phase 3): the seed of the detour shown at the right of the status bar, after its label, in a read-only field that can be selected and copied, drawn as the text of the bar until it has the focus; the card of the route is titled "Detour" when it shows a detour. |
| Map | Applied (phase 1): Leaflet in a `WebView`, bundled (`map/`). While the graph is loaded or imported, a veil (`-c-bg`, dimmed) covers the map, which cannot be clicked, with a card in the middle: a progress indicator, what is happening and, for an import, how long it takes. The page (`map.css`, `map.js`) holds **no colour value**: `Theme` hands it the values of the tokens at load and when the theme changes; the style sheet of Leaflet, bundled unchanged, keeps its own. The attribution of the tile provider and "© OpenStreetMap contributors" stay visible in a corner of the map, in `-c-text-2` on `-c-surface`, as text (no link, which would open in the map). The fastest route is a dashed line `-c-text-3`; start and finish are discs `-c-accent` with a letter (`D` / `A` in French, `S` / `F` in English) and a tooltip, never a colour alone. Zoom with the mouse wheel and the buttons + / −, the keys + / − and the arrows when the map has the focus; the focus of the map is a ring `-c-accent`. Applied (phase 3): the detour a solid line `-c-accent` over the dashed fastest route, both shown whole. Applied (issue #4): the clicks place the start, then the finish, and nothing once both are placed (a click by mistake lost the route); the markers of the start and the finish are dragged to move them (the pointer of Leaflet), and a right-click shows a context menu of JavaFX where the pointer is, "Place the start here" and "Place the finish here" (the context menu of WebKit prevented by the page), "Add a box here" and "Add a waypoint here" (phase 5; a waypoint on the box within 25 m of the place, or on a box added there, the box then selected), "Start a speed zone here" and "Finish the speed zone here" (increment 5: a DZ at 50 km/h or a FZ, placed the same way), disabled without route, the status bar saying when the place is too far from the route; the box selected in the roadbook, but the start and the finish, dragged along the route (its circle follows the line, the pointer `move`), the map centred on a box selected only when it is out of sight; Undo and Redo of the Edit menu bring back the states before. Not done: the context menu from the keyboard (Leaflet gives no place without a pointer). _Proposed_: zoom by Shortcut++ / Shortcut+− from the window. |
| Roadbook boxes | Applied (phase 4, at the request of the maintainer, to check the roadbook against the map): the roadbook of the route shown is the **detail panel** at the right of the map (at its left since phase 5, the editor of the box at its right) (§6, a `SplitPane`, the map keeping 70 % of the width), titled "Roadbook", its number of boxes as a caption, an empty state without route. A roadbook is a printed object, and training means reading the real thing: the boxes are drawn **as on paper** — ink on white in both themes, two tokens of the same value in both files (`-c-paper`, `-c-ink`), the FIA asking for a roadbook "in colour on white background". One box = one row, separated by a hairline of ink: its number, bold, at the left (to find a box again, asked by the maintainer), then the total distance (1.54em, bold) above the partial distance, right-aligned, two decimals in the format of the `Locale` of the application, at the left (as the distance box of the FIA, §5.5); the tulip in the middle (100 px); the heading at the right where the catalogue shows it ("CAP 320", 1.54em, bold). Applied (phase 5): the notes of the box drawn as on paper too: the danger marks (red, the catalogue §14) and the symbols in the tulip, the abbreviations and the free text under the heading, bold, right-aligned, wrapped within 96 px, in `-c-ink`. The box selected, or hovered, stays on paper (not on `-c-selection`, dark in the dark theme, under the ink): it is framed in `-c-accent`, and shown on the map: a circle in the accent at its place, the map centred on it, close enough to read the junction; the arrows of the keyboard move from a box to the next. Each box has its number, its distances and its notes (its danger, its description, the names of its symbols) as accessible text. Applied (phase 5, increment 4): the waypoint of a box, but a WPP (FIA §5.12.2), its letter in a circle under the number, in the colours of the FIA (§5.12.3): ink, or blue for a WPN, on cyan (WPM, WPV), red (WPS), yellow (WPC), blue (WPE) or paper (WPN), tokens of the same value in both files (`-c-wp-cyan`, `-c-wp-blue`, `-c-wp-red`, `-c-wp-yellow`), never a colour alone: the letter says the kind, and the accessible text of the box names it. Applied (increment 5): the speed zones (§5.9): DZ in a circle on orange, FZ on green (`-c-wp-orange`, `-c-wp-green`), their two letters smaller; every box of the zone, from its DZ to its FZ, framed in orange (the frame of the box selected prevails); the sign of the speed limit at the top of the description of the DZ and of the FZ, drawn by the catalogue (an orange ring and the speed, struck out at the FZ), its speed or its end in the accessible text; a DZ inside a zone, which finishes it at the same box, has the circle of a FZ above its own and the two signs side by side, the speed finished struck out first. Not done: the background of the distance box in the colour of the waypoint. _Proposed_: the next box within 300 m (§5.6.11). |
| Symbols | Applied (phase 4, decision of the maintainer: follow the rulebook of the FIA): a catalogue (module `catalog`, one JSON file with SVG paths) holds its own colours, like the icon of the application, outside the tokens of §3 — the roadbook of the FIA is "in colour on white background" (2026 Cross-Country Rally Sporting Regulations, Appendix III, §5.1.3), the route followed in blue `#0070C0` with its arrow (§5.6.1), the other roads in black, a tarmac road a double line, a track a single line (§5.14). A tulip is drawn as on paper, the same in the light and the dark themes. The OpenRally export renders the tulips as PNG of 200 px (Terra Pirata does not show SVG). _Proposed_: in the window, a symbol always has its meaning as accessible text (`setAccessibleText`) and as tooltip. |
| Training | Applied (phase 7a; decisions of the maintainer, 2026-11-20: a mode of the main window, twenty questions of four choices, the answer shown at once; then, as many questions as the elements to revise when they are fewer, twenty at most): Training menu, after the Route menu, "Symbol quiz" (Shortcut+T) shows the quiz in the centre of the window, over the three columns, which stay as they are; "Back to the roadbook" (disabled outside the quiz), the button of the quiz of the same name, or any item of the File, Edit and Route menus but Settings… (they act on the roadbook) brings them back. One page at a time on `-c-surface`, in a column of 560 px at most, centred: the setup (view title "Symbol quiz", a sentence saying how it works, the section "To revise" with a check box per topic and its number of elements, the symbols checked, the mode "Confusions: offer the elements that look alike" and its caption, "Start the session" primary, disabled while no topic is checked with a caption in `-c-danger` saying so, "Back to the roadbook"); one question per page ("Question 3 of 20" and "Right: 2" as captions, the question as a panel title, the element drawn **as on paper** on a square of `-c-paper` — the symbol and the danger mark by the catalogue, the abbreviation as the catalogue writes it, bold, in `-c-ink`, the letters of the waypoint in its circle in the colours of the strip — or its meaning as a view title, then the choices one under the other as buttons of 44 px, numbered "1. Bump", or "1" and the element drawn, taken by a click or the keys 1 to 4 of the row or of the keypad); once answered, the choices disabled at full opacity, the right one framed in `-c-success`, a wrong one taken framed in `-c-danger`, **and** the sentence "Right." or "Wrong. The right answer: 3. Bump." in the same colour (colour never alone, §3), then "Next question" primary (Enter), "See the result" after the last one, and "Stop the session" back to the setup; the progress counts the questions of the session (3 for the danger marks alone); the result as a view title "Session finished" and "16 right out of 20", "New session" primary and "Back to the roadbook". An element shown by its sign has "Element to identify" as accessible text, not its meaning. _Proposed_ (phases 7b to 7d): blind mode, the map hidden by an empty state that says so, not only greyed; the progress with the components of §7 (tables, captions), no gamified decoration. |
| Preferences | Applied (phase 1): a dialog of settings (`SettingsDialog`, §7) in two sections ("Background map", "Navigation on the map"), each field at the right of its label (right-aligned, `-c-text-2`), its caption under it: the provider of the background map (national maps of swisstopo, MapTiler) and the key of MapTiler in a password field, with a caption saying where to get it and where it is kept; the field is disabled for swisstopo and "Save" is disabled while MapTiler has no key; the sensitivity of the zoom to the mouse wheel and to the trackpad (low, medium — the default —, high, very high: one level every 60, 30, 15 or 8 pixels of scroll). JavaFX cannot add an item to the application menu of macOS: "Settings…" (Shortcut+,) in the File menu on macOS, "Options…" in a Tools menu elsewhere. _Proposed_: theme, reduced animations, map extracts; a manual theme opposite to the mode of the system has no colours of the system to take (JavaFX gives only those of the current mode), to be decided with its phase. |
| Persistence | Applied (phase 1): the settings in `settings.properties` of the data folder of the user (`DataFolder`) rather than `java.util.prefs`, so that the tests never touch the preferences of the user. _Proposed_: the state of the window in the same folder. |
| Terms of use at the start | Applied (issue #7): at each start, over the main window, a modal dialog (§7: a decision before using the map) titled "Terms of use"; in the dialog, the title as a view title (1.54em, semi-bold), then three short paragraphs (the data, the rules, the responsibility), 480 px wide, **at 1.15em** — an exception to the body of §4, at the request of the maintainer, so that the terms are read —, line height 1.5 (§4), 16 px between the paragraphs; two buttons of explicit labels, "I accept" (default) and "Refuse and quit" (cancellation: closing the dialog or Escape is the refusal), in the order of the system; no "do not show again". |
| Undo / Redo | Applied (issue #4): Edit menu, after the File menu, "Undo" (Shortcut+Z) and "Redo" (Shortcut+Shift+Z, and Ctrl+Y outside macOS, Windows and Linux alike, as §10 asks it for Windows), disabled when there is nothing to bring back; they bring back the start, the finish and the route shown, the fastest one or a detour with its seed, before each change of the user (a click, a marker moved, a detour generated, a roadbook opened), the last 50; not the settings nor the exports. A route undone while it was computed is computed again when brought back. |
| Menus | File menu: "Open a map…" (Shortcut+O), "Open a roadbook…" (Shortcut+Shift+O, an open dialog limited to the GPX files; the roadbook shown as exported; a failure in an error dialog over the window, §7, titled by the action "Open a roadbook", its heading "The roadbook cannot be opened", its text the reason and how to correct it, a button "OK", the status bar left as it was: in the status bar, the failure was missed), "Open a recent roadbook" (a submenu of the roadbooks opened or saved last, the last one first, each "name — folder", disabled while empty; a roadbook moved or deleted since is removed from it and the user told in a dialog, §7: its heading "The roadbook “name” no longer exists", its text the consequence and how to open it again), "Save the roadbook" (Shortcut+S: into the file of the roadbook, opened or saved last, without asking; the first time as "Save the roadbook as…") and "Save the roadbook as…" (Shortcut+Shift+S, a save dialog limited to the GPX files), and "Export as GPX…" (phase 6: a save dialog "Export as GPX" limited to the GPX files, "route.gpx" proposed; the plain GPX for other applications, which does not become the file of the roadbook), "Export as PDF…" (phase 6: a save dialog "Export as PDF" limited to the PDF files, "roadbook.pdf" proposed; the strip to print, which does not become the file of the roadbook either), all disabled while no route is shown, the result or the failure in the status bar. The title of the window is "name – Box One", "name (modified) – Box One" while the roadbook differs from the file (a roadbook never saved: "New roadbook (modified)" once changed), "Box One" alone otherwise; closing with changes not saved asks "Save the changes of the roadbook?" (Save, Don't Save, Cancel, §7), and a roadbook being saved is saved before the application closes; then "Settings…" on macOS, where the application menu quits, or Quit (Shortcut+Q) elsewhere, with a Tools menu holding "Options…". Route menu (phase 3): "Generate a detour…" (Shortcut+D, disabled while no route is shown), a dialog (§7) with the budget (its amount, "0 min" to "60 min" or "5 %" to "100 %" by steps of 5, then its unit, "In minutes" or "In percent of the fastest route"; a change of the unit selects the nearest budget; for a percentage, the caption gives the time on the fastest route), the kinds of road to avoid (check boxes "Motorways", "Expressways", "Ferries and car trains", "Toll roads", those of the last detour checked, kept in the settings; the caption says that the fastest route avoids them too and that the motorways of the vignette are toll roads) and the seed (a new one proposed, 1 to 6 digits; otherwise the field is marked, its caption says so and "Generate" is disabled), each with its caption; the progress and the failure in the status bar. Training menu (phase 7a): "Symbol quiz" and "Back to the roadbook" (row Training). |
| Icons | Home-made line icons (`SVGPath`), no Ikonli dependency, as soon as the first icon is needed. |
| Icon of the application | Applied: drawn by the maintainer, `ui/src/packaging/icons/box-one.svg` (1024 px, a rounded square of the night blue `#13212F`, a junction of a tulip in cream `#F4EFE5`, the box number 1 in an orange disc `#F26B1D`); its colours belong to the icon, outside the tokens of §3, as for every application. Rendered once from the SVG (Inkscape) into `box-one.icns` (`iconutil`, 16 to 1024 px), `box-one.ico` (16 to 256 px) and `box-one.png` (512 px) for `jpackage`, and into PNG of 16 to 256 px for the window (`MainController.icons()`, title bar and task bar on Windows and Linux; the Dock of macOS takes the `.icns`). It fills its square: macOS draws its icons inside a margin of about 10 %, so it may look larger than its neighbours in the Dock. |
| Not applied yet | Everything except the frame of the main window (menus, map, status bar, theme of the system) and the settings of the background map: toolbar, side navigation, detail panel, other settings (manual theme), restoration of the window, icons of the controls, motion, mnemonics of the buttons, About; tabular figures (not available in the CSS of JavaFX); the contrast of 3:1 of the borders of the controls (§3: `-c-border-strong` is below it, about 1.8:1 on a white surface, as discreet as the borders of the systems and as the former palette). |
