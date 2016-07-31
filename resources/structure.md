```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                           216           3940           1178          22680
-------------------------------------------------------------------------------
SUM:                           216           3940           1178          22680
-------------------------------------------------------------------------------
└── diskbrowser
    ├── applefile
    │   ├── AbstractFile.java
    │   ├── AppleFileSource.java
    │   ├── ApplesoftConstants.java
    │   ├── AssemblerConstants.java
    │   ├── AssemblerProgram.java
    │   ├── AssemblerStatement.java
    │   ├── BasicProgram.java
    │   ├── BootSector.java
    │   ├── CPMTextFile.java
    │   ├── Charset.java
    │   ├── Command.java
    │   ├── DefaultAppleFile.java
    │   ├── ErrorMessageFile.java
    │   ├── HiResImage.java
    │   ├── IconFile.java
    │   ├── IntegerBasicProgram.java
    │   ├── LodeRunner.java
    │   ├── MerlinSource.java
    │   ├── PascalCode.java
    │   ├── PascalCodeStatement.java
    │   ├── PascalConstants.java
    │   ├── PascalInfo.java
    │   ├── PascalProcedure.java
    │   ├── PascalSegment.java
    │   ├── PascalText.java
    │   ├── ShapeTable.java
    │   ├── SimpleText.java
    │   ├── SimpleText2.java
    │   ├── StoredVariables.java
    │   ├── TextBuffer.java
    │   ├── TextFile.java
    │   ├── VisicalcFile.java
    │   ├── WizardryTitle.java
    │   └── equates.txt
    ├── appleworks
    │   ├── AppleworksADBFile.java
    │   ├── AppleworksSSFile.java
    │   ├── AppleworksWPFile.java
    │   ├── Cell.java
    │   ├── CellAddress.java
    │   ├── CellConstant.java
    │   ├── CellFormat.java
    │   ├── CellFormula.java
    │   ├── CellLabel.java
    │   ├── CellValue.java
    │   ├── LabelReport.java
    │   ├── Record.java
    │   ├── Report.java
    │   └── TableReport.java
    ├── catalog
    │   ├── AbstractCatalogCreator.java
    │   ├── AbstractDiskCreator.java
    │   ├── CatalogLister.java
    │   ├── DiskLister.java
    │   ├── DocumentCreatorFactory.java
    │   ├── TextCatalogCreator.java
    │   └── TextDiskCreator.java
    ├── cpm
    │   ├── CPMCatalogSector.java
    │   ├── CPMDisk.java
    │   └── DirectoryEntry.java
    ├── disk
    │   ├── AbstractFormattedDisk.java
    │   ├── AbstractSector.java
    │   ├── AppleDisk.java
    │   ├── AppleDiskAddress.java
    │   ├── DataDisk.java
    │   ├── DefaultAppleFileSource.java
    │   ├── DefaultDataSource.java
    │   ├── DefaultSector.java
    │   ├── Disk.java
    │   ├── DiskAddress.java
    │   ├── DiskFactory.java
    │   ├── DualDosDisk.java
    │   ├── FormattedDisk.java
    │   ├── SectorList.java
    │   ├── SectorListConverter.java
    │   └── SectorType.java
    ├── dos
    │   ├── AbstractCatalogEntry.java
    │   ├── CatalogEntry.java
    │   ├── DeletedCatalogEntry.java
    │   ├── DosCatalogSector.java
    │   ├── DosDisk.java
    │   ├── DosTSListSector.java
    │   └── DosVTOCSector.java
    ├── gui
    │   ├── AboutAction.java
    │   ├── AbstractTab.java
    │   ├── AppleDiskTab.java
    │   ├── CatalogPanel.java
    │   ├── CloseTabAction.java
    │   ├── ColourQuirksAction.java
    │   ├── CreateDatabaseAction.java
    │   ├── DataPanel.java
    │   ├── DataSource.java
    │   ├── DebuggingAction.java
    │   ├── DiskAndFileSelector.java
    │   ├── DiskBrowser.java
    │   ├── DiskDetails.java
    │   ├── DiskLayoutImage.java
    │   ├── DiskLayoutPanel.java
    │   ├── DiskLayoutSelection.java
    │   ├── DiskLegendPanel.java
    │   ├── DiskSelectedEvent.java
    │   ├── DiskSelectionListener.java
    │   ├── DuplicateAction.java
    │   ├── ExecuteDiskAction.java
    │   ├── FileNodeSelectedEvent.java
    │   ├── FileNodeSelectionListener.java
    │   ├── FileSelectedEvent.java
    │   ├── FileSelectionListener.java
    │   ├── FileSystemTab.java
    │   ├── HideCatalogAction.java
    │   ├── HideLayoutAction.java
    │   ├── InterleaveAction.java
    │   ├── LineWrapAction.java
    │   ├── MenuHandler.java
    │   ├── MonochromeAction.java
    │   ├── NoDisksFoundException.java
    │   ├── OpenFileAction.java
    │   ├── PreferencesAction.java
    │   ├── PreferencesDialog.java
    │   ├── PrintAction.java
    │   ├── PrintDocument.java
    │   ├── RedoHandler.java
    │   ├── RefreshTreeAction.java
    │   ├── RootDirectoryAction.java
    │   ├── ScrollRuler.java
    │   ├── SectorSelectedEvent.java
    │   ├── SectorSelectionListener.java
    │   ├── ShowFreeSectorsAction.java
    │   ├── Tab.java
    │   ├── TreeBuilder.java
    │   └── WindowSaver.java
    ├── icons
    │   ├── Symbol-Left-32.png
    │   ├── Symbol-Right-32.png
    │   ├── arrow_refresh.png
    │   ├── arrow_refresh_32.png
    │   ├── disk.jpg
    │   ├── disk.png
    │   ├── disk2.png
    │   ├── folder_explore_16.png
    │   ├── folder_explore_32.png
    │   ├── font_add_32.png
    │   ├── font_delete_32.png
    │   ├── information_16.png
    │   ├── information_32.png
    │   ├── printer_16.png
    │   ├── printer_32.png
    │   ├── save_delete_16.png
    │   ├── save_delete_32.png
    │   └── script_gear_32.png
    ├── infocom
    │   ├── Abbreviations.java
    │   ├── AttributeManager.java
    │   ├── CodeManager.java
    │   ├── Dictionary.java
    │   ├── Globals.java
    │   ├── Grammar.java
    │   ├── Header.java
    │   ├── InfocomAbstractFile.java
    │   ├── InfocomDisk.java
    │   ├── Instruction.java
    │   ├── ObjectAnalyser.java
    │   ├── ObjectManager.java
    │   ├── PropertyManager.java
    │   ├── PropertyTester.java
    │   ├── Routine.java
    │   ├── StringManager.java
    │   ├── ZObject.java
    │   └── ZString.java
    ├── pascal
    │   ├── CatalogEntry.java
    │   ├── FileEntry.java
    │   ├── PascalCatalogSector.java
    │   ├── PascalCodeObject.java
    │   ├── PascalDisk.java
    │   └── VolumeEntry.java
    ├── prodos
    │   ├── CatalogEntry.java
    │   ├── DirectoryHeader.java
    │   ├── FileEntry.java
    │   ├── ProdosBitMapSector.java
    │   ├── ProdosCatalogSector.java
    │   ├── ProdosConstants.java
    │   ├── ProdosDirectory.java
    │   ├── ProdosDisk.java
    │   ├── ProdosExtendedKeySector.java
    │   ├── ProdosIndexSector.java
    │   ├── SubDirectoryHeader.java
    │   └── VolumeDirectoryHeader.java
    ├── utilities
    │   ├── DateTime.java
    │   ├── FileFormatException.java
    │   ├── HexFormatter.java
    │   ├── LZW.java
    │   ├── LZW1.java
    │   ├── LZW2.java
    │   ├── NuFX.java
    │   ├── Thread.java
    │   └── Utility.java
    ├── visicalc
    │   ├── Abs.java
    │   ├── Address.java
    │   ├── And.java
    │   ├── Cell.java
    │   ├── Condition.java
    │   ├── Count.java
    │   ├── Error.java
    │   ├── Expression.java
    │   ├── Function.java
    │   ├── If.java
    │   ├── Int.java
    │   ├── IsError.java
    │   ├── IsNa.java
    │   ├── Lookup.java
    │   ├── Max.java
    │   ├── Min.java
    │   ├── Na.java
    │   ├── Npv.java
    │   ├── Number.java
    │   ├── Or.java
    │   ├── Pi.java
    │   ├── Range.java
    │   ├── Sheet.java
    │   ├── Sum.java
    │   └── Value.java
    └── wizardry
        ├── AbstractImage.java
        ├── Character.java
        ├── CodedMessage.java
        ├── Dice.java
        ├── ExperienceLevel.java
        ├── Header.java
        ├── Image.java
        ├── ImageV2.java
        ├── Item.java
        ├── MazeAddress.java
        ├── MazeCell.java
        ├── MazeLevel.java
        ├── Message.java
        ├── Monster.java
        ├── PlainMessage.java
        ├── Reward.java
        ├── Spell.java
        └── WizardryScenarioDisk.java

15 directories, 235 files
```