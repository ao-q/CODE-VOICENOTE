# Voice Notes: Minimal Mode, Drawing Re-Access, Custom Emoji Tags & Dual Sync (Revised)

A streamlined update to Voice Notes that strictly eliminates emoji decorations across standard app text and dialogs, enforces clean icon-only controls without redundant text descriptions, revives complete re-access and editing of saved canvas drawings via a dedicated drawing modal, adds a '+' category in the file sorter to assign custom emoji tags to notes, implements dual Sync options (Import from Device and Save/Load Settings JSON), removes the title dot, and enables an ultra-compact Minimal mode toggled from the title.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and design decisions incorporate the latest user revision:

- **No Emojified Text**: All standard text, headings, dialog bodies, and alerts across the app are clean and plain text without decorative emojis. Custom emojis are strictly confined to note tags in the file sorter and note tag badges.
- **Strict Icon-Only Controls**: Wherever an action or toggle has an icon (Edit/Preview toggles, action buttons, sync options, toolbar controls), text labels and descriptions are completely omitted, maintaining a clean, minimalist interface with appropriate accessibility content descriptions.
- **Drawing Re-Access & Editing**: Saved canvas drawings open in a dedicated drawing modal featuring a full preview and direct icon actions for editing on the canvas and lossless export.
- **Minimal Mode**: Tapping the "Voice Notes" title (blue dot removed) toggles a dense, compact layout with streamlined cards and reduced padding.
- **Settings & File Sync**: Dual sync dialog provides direct icon-first actions for importing device files and saving/loading offline settings JSON.

---

## 1. Overview & Core Concept

### What It Does
1. **Icon-Only Experience**: Removes text descriptions wherever icons exist (such as the Edit/Preview pills in note detail and recording pane, and secondary action bars).
2. **Text De-Emojification**: Removes decorative emojis from all titles, buttons, prompts, and dialog text across the entire application.
3. **Canvas Drawing Re-Access**: Tapping on a drawing note displays the drawing modal with icon-only actions to edit in the canvas board or export.
4. **Custom Emoji Note Tags**: In the file sorter row, users can tap a '+' chip to define a custom tag with an emoji and assign it to specific notes for rapid filtering.
5. **Dual Sync Modal**: Clicking the sync button opens two clean options:
   - Sync from Device (with '+' icon) to import `.txt`, `.mp3`, and `.md` files.
   - Sync Settings with Load and Save options for offline JSON backup of user preferences, tags, and assignments.
6. **Title Minimal Mode**: Clicking the "Voice Notes" title (without any dot) switches the app into a compact theme.

### Target Audience & Persona
Users seeking a rapid, tactile note-taking and voice memo tool with zero visual noise, no redundant descriptions, and fast visual categorization.

### Key Value
Clean visual hierarchy, complete continuity for stylus/finger drawings, and streamlined, icon-first navigation.

---

## 2. User Experience & Visual Design

### Key User Flows

#### Flow 1: Drawing Note Re-Access & Canvas Resumption
```
Note List / Canvas Sorter
       │
       ▼ (Tap drawing card)
┌──────────────────────────────────────────────┐
│                Drawing Modal                 │
│  [High-Resolution Drawing Canvas Preview]    │
│                                              │
│  [ ✏️ ]              [ 📤 ]           [ ✕ ]  │
└──────────────────────────────────────────────┘
       │
       ▼ (Tap ✏️ Edit)
HandwrittenCanvasBoard (pre-loaded with drawing bitmap for continued sketching)
       │
       ▼ (Make edits & tap Save)
Updated PNG & note record written directly to disk
```

#### Flow 2: Custom Tag Creation & File Sorter
```
File Sorter Row: [ 📁 ] [ 🎙️ ] [ 📝 ] [ 🎨 ] [ ➕ ]
                                               │
       ┌───────────────────────────────────────┘
       ▼
┌──────────────────────────────────────────────┐
│                 New Tag                      │
│  [⭐] [💡] [💼] [🎓] [🚀] [📌] [ 🏷️ ]          │
│  Tag name input field                        │
│  [ Cancel ]                     [ Save ]     │
└──────────────────────────────────────────────┘
       │
       ▼
File Sorter Row updates: [ 📁 ] [ 🎙️ ] [ 📝 ] [ 🎨 ] [ ⭐ ] [ ➕ ]
Card menu provides "Assign Tag" to attach emoji tag to note.
Tapping [ ⭐ ] filters list exclusively to matching notes.
```

#### Flow 3: Dual Sync Modal (Icon-First)
```
Top App Bar Sync Icon Button
       │
       ▼
┌────────────────────────────────────────────────────────┐
│                      Sync                              │
│                                                        │
│  [ ➕ ]  Device Files                                  │
│         Imports txt, mp3, md                           │
│                                                        │
│  [ ⚙️ ]  Settings                                      │
│         [ 💾 Save ]        [ 📂 Load ]                 │
└────────────────────────────────────────────────────────┘
       │
       ├─► Device Files ──► Recursive import of all supported formats
       │
       └─► Settings
             ├─► Save ──► Exports offline JSON settings backup
             └─► Load ──► Imports offline JSON settings backup
```

#### Flow 4: Compact Minimal Mode
- User clicks "Voice Notes" in TopAppBar (no blue dot).
- Densely packed layout activates:
  - Tight card padding (6.dp) and single-line note titles.
  - Condensed player bar with icon-only playback buttons.
  - Minimized vertical margins.
- Tapping the title again restores the standard spacious view.

### Visual Identity & Theme
- **Strict Icon-First Principle**: When an icon communicates the action, omit text labels.
- **De-Emojified Body & Interface Text**: Zero emoji prefixes in headings, dialogs, button labels, or status toasts.
- **Dedicated Tag Role for Emojis**: Emojis are reserved strictly for note classification chips and category badges.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Text Descriptions Removal**
  - *Chosen Approach*: Replace dual Icon + Text toggle buttons (such as Edit / Preview) with icon-only pill segments equipped with semantic `contentDescription` for accessibility.
  - *Why*: Directly aligns with user requirement to eliminate visual text clutter and duplicate descriptions.
  - *Alternatives Considered*: Retaining small text labels (rejected per user request).

- **Decision 2: Elimination of Decorative Emojis**
  - *Chosen Approach*: Audit all strings, dialog titles, and card headers to remove non-functional emojis, keeping emojis solely as user-created category tag identifiers in the file sorter.
  - *Why*: Produces a professional, distraction-free aesthetic.

- **Decision 3: Drawing Re-Access via Dedicated Modal**
  - *Chosen Approach*: When a drawing note is clicked, present an inspection modal with icon actions (Edit, Export, Close) rather than routing into the markdown text editor.
  - *Why*: Drawings are graphical assets; opening them in a text editor caused broken workflow.

- **Decision 4: Settings File Format & Persistence**
  - *Chosen Approach*: JSON serialization via Android storage access framework (`CreateDocument` / `OpenDocument`), preserving theme, compact mode, storage folder, and custom emoji tags.
  - *Why*: Robust, human-readable, and offline-compatible.

---

## 4. Technical Architecture & Data Strategy

### Component Hierarchy & Interaction Flow

```
┌───────────────────────────────────────────────────────────────────────────┐
│                           MainVoiceNotesScreen                            │
│                                                                           │
│  TopAppBar                                                                │
│   ├── Title: "Voice Notes" (clickable, no dot -> toggles Minimal Mode)    │
│   ├── Theme Toggle (Light/Dark icon)                                      │
│   ├── New Folder (Folder icon)                                            │
│   ├── Search (Magnifier icon)                                             │
│   └── Sync Button (Sync icon) ───────────────────┐                        │
│                                                  ▼                        │
│                                      ┌──────────────────────┐             │
│                                      │   SyncChoiceDialog   │             │
│                                      │  [+] Device Files    │             │
│                                      │  [⚙] Settings        │             │
│                                      │      [💾] Save       │             │
│                                      │      [📂] Load       │             │
│                                      └──────────────────────┘             │
│                                                                           │
│  WriteNotesSection                                                        │
│   ├── Quick Actions: [Mic icon] [Note icon] [Draw icon] (Icon-only)       │
│   ├── File Sorter: [📁] [🎙️] [📝] [🎨] [Custom Emoji Tags] [➕]            │
│   │                                                        │              │
│   │                                                        ▼              │
│   │                                                CreateTagDialog        │
│   │                                                                       │
│   └── Note List (Standard / Densely Packed Minimal Mode)                  │
│        ├── Standard / Compact Note Cards (Icon-only actions)              │
│        └── Drawing Note Cards (🎨) ──────────────┐                        │
│                                                  ▼                        │
│                                      ┌──────────────────────┐             │
│                                      │ DrawingDetailDialog  │             │
│                                      │  - Preview Bitmap    │             │
│                                      │  - [✏️] Edit Canvas   │             │
│                                      │  - [📤] Export        │             │
│                                      └──────────┬───────────┘             │
│                                                 │                         │
│                                                 ▼                         │
│                                      HandwrittenCanvasBoard               │
│                                     (Populated with drawing)              │
│                                                                           │
│  NoteDetailScreen / ActiveRecordingPane                                   │
│   └── Edit / Preview Toggles: [✏️] [👁️] (Icon-only, no text labels)       │
└───────────────────────────────────────────────────────────────────────────┘
```

### Data & State Model

1. **Tag Management**:
   - `CustomNoteTag(id: String, emoji: String, name: String, createdAt: Long)`
   - Stored in `StoragePreferences` and synced with Room entities.

2. **JSON Settings Backup Structure**:
   - `isDarkTheme: Boolean`
   - `isMinimalMode: Boolean`
   - `storageFolderName: String`
   - `customTags: List<CustomNoteTag>`
   - `tagAssignments: Map<Long, List<String>>`
   - `version: 1`

3. **Drawing State**:
   - `selectedDrawingNote: VoiceNoteEntity?`
   - `showDrawingModal: Boolean`
   - Bitmap loader decoding directly from `noteFilePath`.
