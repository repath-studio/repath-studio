# Changelog

## 0.4.12 (2025-01-09)

**Added**

- Multi-touch support [116](https://github.com/repath-studio/repath-studio/pull/116)
- Radius handle for rect elements [#106](https://github.com/repath-studio/repath-studio/pull/106)
- Brush point editing
- Arrow key modifiers (ctrl, shift) to reduce or increase speed on transform
- Missing titles and translations
- Edit tree element label on `E` down

**Changed**

- Enhance resizable panels [114](https://github.com/repath-studio/repath-studio/pull/114)
- Enhance readability of canvas tool overlays
- Improve keyboard accessibility of element tree
- Set default theme mode to dark

**Fixed**

- Print document on desktop
- Cached tool on cancel action
- Order of `stroke->path` keyboard shortcut
- Adding animations
- Missing `window-restored` event

## 0.4.11 (2025-11-21)

**Added**

- Language registry [#102](https://github.com/repath-studio/repath-studio/pull/102)
- Accessibility filter registry [#100](https://github.com/repath-studio/repath-studio/pull/100)
- Mobile enhancements [#95](https://github.com/repath-studio/repath-studio/pull/95)
- Menubar indicator on alt down and open menu on shortcut
- History tree legend
- Italian, Dutch, Swedish translations
- Loading icon placeholder
- Save handling on unsupported browsers
- Missing translations

**Removed**

- FPS indicator from debug info

**Changed**

- Set default theme to system
- Hide snap indicator on select mode
- Enhance popover info styles

**Fixed**

- Locked ratio when creating rect or svg elements
- Protanopia filter values
- Scaling elements with no bbox

## 0.4.10 (2025-10-19)

**Added**

- Persist file handles to open recent and save open files on future sessions [#91](https://github.com/repath-studio/repath-studio/pull/91)
- Code sign and notarize macOS build
- Auto updates
- Preview fill color on pointer over element
- Error reporting dialog
- PWA install button
- Korean and Turkish languages
- Toast notifications [#92](https://github.com/repath-studio/repath-studio/pull/92)
- Icon loading placeholder

**Removed**

- Default canvas fill attribute value

**Changed**

- Expand parent on element create
- Improve behavior of tree item label
- Hide fullscreen button on desktop app
- Hide loader on electron app
- Increase default zoom sensitivity
- Icon update
- Hide disabled commands from command panel
- Set default theme mode to system
- Simplify dialogs
- Set cursor to not-allowed when all elements are locked
- Enhance outline styles
- Show tree item prop toggles on focus
- Update boolean operation icons

**Fixed**

- Context menu escape propagation
- Various translation issues
- Update image size on image href update
- Polygon and polyline drag end

## 0.4.9 (2025-9-4)

**Added**

- Export to rasterized image formats (png, jpeg, bmp, gif, webp)
- System language option
- More languages (French, German, Spanish, Russian, Chinese, Portuguese, Japanese)
- Top level language dropdown
- Subsequent file saves support on web after loading/saving
- Opening multiple files at once on web
- Basic progressive web app (PWA) support
- Shortcuts to tooltips
- Logo to home
- Decoding and crossorigin attribute dropdown
  
**Removed**

- Help text on mobile

**Changed**

- Enhance on focus of attribute input
- Handle escape on transform key down
- Enhance color picker styles
- Prevent multiselect with root
- Enhance loading state
- Enhance mobile dialog styles
- Enable menubar scrolling
- Always display fullscreen toggle
- Enhance ruler pointer style
- Enhance persist error handling
- Move worker loading indicator on top of canvas

**Fixed**

- Document pan on application load
- Various language direction issues
- Attribute key normalization
- History tree jittering on pan
- Auto adjust size of canvas labels
- Codemirror theme mode, disabled state, multiple values
- Adjusted point of polygon and polyline
- Focus on text create
- SVG pointer down event

## 0.4.8 (2025-7-3)

**Added**

- Internationalization support [#87](https://github.com/repath-studio/repath-studio/pull/87)
- Initial attribute value support
- Reintroduce FPS component
- Core attributes to container elements

**Changed**

- Font enhancements [#84](https://github.com/repath-studio/repath-studio/pull/84)
  - Converting text elements to paths works on web
  - Noto Sans bundled with the app
  - Text elements without a font family set can be converted to paths
  - Font weight filtering based on the selected font-family also works on web
  - Improved scaling of text elements
- Persist enhancements [#86](https://github.com/repath-studio/repath-studio/pull/86)
  - Persisting is not constrained to local storage limitations
  - Some loading issues on we are now fixed
- Tool enhancements [#85](https://github.com/repath-studio/repath-studio/pull/85)
  - Preview attributes while creating elements
  - Disable attributes while modifying elements
- Enhance performance
- Enhance cursors
- Enhance button styles
- Increase handle size

**Fixed**

- Refresh bbox on set parent
- Various precision issues
- Multi-selection label
- Stop propagation of escape on various cases
- XML view id console warnings

## 0.4.7 (2025-5-17)

**Changed**

- Set label cursor to text only when the item is selected
- Move help messages over canvas
- Simplify and improve performance of tests
- Enhance close button styles
- Enhance pan help messages

**Fixed**

- Accessibility issues
- Polygon and polyline description
- loadURL on windows

## 0.4.6 (2025-5-7)

**Added**

- Clipboard-write error handling
- Ellipse area method
- Animation icons

**Removed**

- cljs-ajax dependency

**Changed**

- Stroke width of bounding box
- Enhanced history preview
- Unlock element on create/copy/duplicate
- Svg tree label font weight

**Fixed**

- Recenter to dom rect
- Adjusted bounds on nested containers
- Multiple image selection error
- Timeline animations
- Saved and close event
- Simplify history drop-rest
- Persist effect
- Image tool drag end
- Browser compatibility overflow
- Drag start on canvas
- Grid index

## 0.4.5 (2024-11-05)

**Added**

- Snap while creating or measuring elements
- Snap labels
- Highlight snapped object

**Changed**

- Normalize all element attributes
- Improve snap performance
- Enhance readability of debug overlay
- Hide bounding corner handles while scaling

**Fixed**

- Snap with locked proportions or restricted direction
- Icon on home page
- Path manipulation actions
- Fill tool
- Pan to element

## 0.4.4 (2024-10-14)

**Added**

- Icons to element tree
- Select element range on tree shift click
- Default font weights

**Removed**

- Blocking loader

**Changed**

- Enhance performance
- Enhance readability of attribute info
- Enhance scale behavior

**Fixed**

- Multiselect on tree
- Fill/stroke picker offset
- Keyboard navigation on element tree
- Form input width
- Disabled state of font family popover

## 0.4.3 (2024-09-23)

**Added**

- Font preview on font select
- Persisting snap options
- System language
- Initial attr value for browsers
- Portfolio
- Default document title on save dialog

**Removed**

- Dropper error message
- Id, title and saved from saved documents

**Changed**

- Enhance brush cursor
- Enhance square-handle styles
- Simplify select messages
- Enhance attribute info on browsers
- Persist the last history state only
- Performance enhancements

**Fixed**

- Document migration
- Local storage clear and relaunch
- Mobile pointer events
- Deleting text element when there is no content
- Avoid creating empty paths after bool operation
- Persist on canvas zoom
- Mobile app height
- Clearing temp element on deactivation
- Multiple file load
- Saved indicator
- Disabled state on attributes
- Minor icon issues

## 0.4.1 (2024-08-30)

**Added**

- Document migrations

**Changed**

- Switch to uuids

## 0.3.0 (2024-08-29)

**Added**

- System font select on supported browsers
- Auto center on resize
- Document templates and icons to home
- Print document
- Missing icons
- Scaling children while holding alt
- Persist documents on local storage
- Version to db and clear storage on incompatible versions

**Removed**

- Sentry integration

**Changed**

- Use `:id` instead of `:key` to avoid shadowing
- Enhance image performance
- Scrollbars
- Enhance mobile view
- Enhance tool messages
- Enhance recent view on home
- Reduce auto-pan threshold
- Update CONTRIBUTING
- Enhance error handling

**Fixed**

- Auto center on load
- Attributes of multiple selected elements
- Select all
- Title-bar saved indicator
- Brush bounds
- Style attribute on render to string
- Eye dropper tool
- Dynamic snap threshold based on zoom level
- Dialog title accessibility
- Set parent locked condition
- Group element bounds

## 0.2.13 (2024-08-13)

**Added**

- Copy to system clipboard
- System theme option
- Spec validation
- Various icons
- Per tag attribute dispatch
- Font family attribute search
- Font weight attribute dropdown

**Changed**

- Small icons eliminated
- Minor mobile fixes
- Default panel sizes
- Document saved info
- Enhance attribute info card

**Fixed**

- Deleting nested selected elements
- Group index
- Fullscreen event listener
- String drop to canvas
- Dialog markup
- Recent documents order
- Image trace
- Initial value and disabled state of attribute

## 0.2.12 (2024-07-31)

**Added**

- Multiple dialogs support
- Disabled state of menubar items
- Stroke attribute to line

**Removed**

- Resize handles of tree and properties panel
- Ruler lock button

**Changed**

- Enhance item tree arrow handling
- Enhance untitled document handling
- Enhance dialog a11y
- Enhance closing multiple unsaved documents
- Move status text to canvas
- Use setPointerCapture to maintain cursor styles

**Fixed**

- Brush pressure
- Window document title
- Parent element after boolean operation
- Element tree overflow
- Pointer leave on element tree

## 0.2.11 (2024-06-19)

**Removed**

- Image triangulation

## 0.2.10 (2024-06-19)

**Added**

- Various icons

**Removed**

- kibit development dependency

**Changed**

- Project name
- Simplify light theme

**Fixed**

- Selection on animate
- Paste in place selection
- Parent of pasted elements
- Saved document subs
- Popover arrow
- Attribute grid styles

## 0.2.9 (2024-04-22)

**Added**

- Image tracing
- Image triangulation
- SVG import
- Snap options
- Path manipulations (simplify, smooth, flatten, reverse)
- Brush circle pointer
- Open document on drop
- Various icons
- Web workers and loading indicator

**Removed**

- Element-to-path dependency
- Tooltip arrow

**Changed**

- Allow selecting svg elements on drag
- Enable chromium devtools on prod
- Enhance focus styles
- Rearrange toolbars and menus
- Move tool help text to statusbar
- Enhance a11y
- Enhance performance

**Fixed**

- Set-zoom event
- Render to string
- Default modal focus
- Text element creation
- Point parsing
- Prevent copying empty selection
- Recent documents on home

## 0.2.8 (2024-04-09)

**Added**

- Snapping to points
- Image drop
- Export for browsers
- Open and download file for legacy browsers
- Multi-select on file open

**Changed**

- File association
- Enhanced error notifications and dialogs
- Replaced shortcuts with command dialog
- Remove ctrl from multiselect

**Fixed**

- Tree item double click area
- Attribute order
- Prevent default on canvas

## 0.2.7 (2024-03-15)

**Fixed**

- Image sources
- Shortcuts
- Git version

## 0.2.6 (2024-03-15)

**Added**

- Keyboard shortcuts dialog
- About dialog
- Close and save confirmation dialog
- Command button to menubar
- Path editing (wip)

**Changed**

- Enhance command dialog
- Refine shortcuts
- Refactor toolbar styles
- Enhance popovers

**Fixed**

- Menu order
- Dot icon

## 0.2.5 (2024-03-05)

**Added**

- Recent documents to home page
- close saved and containing directory actions to document menu
- Disabled state on context menu items
- Documents menu

**Changed**

- Rename frame events
- Enhance slider
- Enhance window controls
- Enhance file operations

**Fixed**

- Nil recent paths
- App icon margin
- Undo/redo dom structure
- Select content max height
- Setting parent of non-existing element
- Missing default element attrs
- Window keyboard listeners

## 0.2.4 (2024-03-03)

**Added**

- Support save/load file on supported browsers
- Recent open documents
- Pull request template

**Changed**

- Enhance home page
- Enhance undo/redo styles

**Fixed**

- Document title

## 0.2.3 (2024-03-01)

**Fixed**

- Electron log
- Electron reloader
- Default panel size

## 0.2.2 (2024-03-01)

**Added**

- File save/load
- Auto reload on main changes

**Changed**

- Url handling
- Refactor element to path

**Fixed**

- History tree rendering
- Element selection

## 0.2.1 (2024-02-26)

**Changed**

- Logo

**Fixed**

- Resizable panels

## 0.2.0 (2024-02-22)

**Added**

- Timeline module
- History tree module
- Notification module
- Error boundary component
- Pressure-sensitive brush tool
- Select similar objects
- Multi-element resize on anchor point
- Theme switch
- Persist workspace configuration
- Introduce centroid (wip)
- Translation module (wip)

**Removed**

- Page element
- Most google closure deps
- Fluentui dependency

**Changed**

- UI rewrite
- Extended refactoring
- Enhance all tools
- Enhance menubar
- Enhance user repl commands

**Fixed**

Too many fixes to list.

## 0.1.3 (2022-05-09)

**Added**

- deps.edn to add de-dupe dependency
- matrix chat to README.md
- new page button action
- overlay color
- element selection to document history
- history icon and button
- XML view and code icon
- double-click multimethod
- select drag-end default case
- shape functions to user namespace
- edit multimethod and state
- select-box component
- multi-element scaling support

**Changed**

- history select background
- xml and history moved under documents
- package upgrades
- page element and render to string method
- radius calculation
- attribute module refactoring
- dropper tool refactoring
- rotate, history, ungroup icons
- cancel event enhanced
- maintain default state on zoom, pan, ruler
- banner image
- bound operations refactoring
- scale multimethod
- move state renamed
- zoom tool enhanced

**Removed**

- selected method to fix a build error
- mouse over canvas flag
- xml-formatter dependency
- mouse click simulation from canvas

**Fixed**

- default background value of page
- render to string method
- update codemirror on value change
- export to svg
- bounds and size visibility
- align method
- select tool

## 0.1.2 (2022-04-09)

**Added**

- out of canvas mouse tracking and and auto-panning
- active document interceptor
- clj-holmes.yml
- CONTRIBUTING.md
- help function to user namespace
- animation icons

**Changed**

- events and subs
- bcd svg spec
- bounds multimethod
- upgrade electron
- drag-end method of select tool
- move multimethod renamed to translate
- transform method simplified
- README.md

**Fixed**

- ellipse translate method

## 0.1.1 (2022-03-19)

**Added**

- document title to app header
- paste in position
- multiple commands to user namespace
- right click check
- ungroup event
- ruler tool
- fill keyboard shortcut
- keyboard shortcuts for multiple tools
- dropper tool

**Removed**

- reagent and re-frame from bootstrap entries
- re-frame events from repl completions
- path button

**Changed**

- import/export buttons disabled
- hide browser compatibility data
- mouse event simplified
- button styles
- dropper tool
- image tool
- packages upgraded

**Fixed**

- monospace font
- drag page method
- delete and ungroup methods
- shortcuts order
- no accessibility filter selection
- zoom validation

## 0.1.0 (2022-03-6)

**Added**

- Initial proof of concept.
