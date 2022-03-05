/**
 * File: Controller.java
 * Names: Dylan Tymkiw, Alex Yu, Jasper Loverude
 * Class: CS 361
 * Project 4
 * Date: February 28th
 */

package proj4TymkiwYuLoverude;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.event.Event;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;

import org.fxmisc.richtext.StyleClassedTextArea;

/**
 * Controller class serves as the controller for all GUI elements.
 *
 * @author Dylan Tymkiw
 * @author Alex Yu
 * @author Jasper Loverude
 */
public class Controller {

    /**
     * Final String[] KEYWORDS stores all java keywords
     * */

    private static final String[] KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "var"
    };

    // Declare patterns to use with highlighting method
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"   // for whole text processing (text blocks)
            + "|" + "/\\*[^\\v]*" + "|" + "^\\h*\\*([^\\v]*|/)";  // for visible paragraph processing (line by line)
    private static final String INT_PATTERN = "0|1|2|3|4|5|6|7|8|9";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<INT>" + INT_PATTERN + ")"
    );

    // HashMap that hashes tab object to string tab contents
    private HashMap<Tab, String> savedTabsHashMap;
    // Hashmap that hashes tab object to file object
    private HashMap<Tab, File> tabFileHashMap;
    // Field storing thread executor for java keyword detection thread
    private final ExecutorService executor;
    // Field storing stage object of application
    private Stage currentStage;

    @FXML
    private TabPane tabPane; // Pane of tabs in the application
    @FXML
    private Button Hello; // Hello button in the ToolBar
    @FXML
    private MenuItem close; // close menu item
    @FXML
    private MenuItem save; // save menu item
    @FXML
    private MenuItem saveAs; // save as menu item
    @FXML
    private MenuItem selectAll; // save all menu item
    @FXML
    private MenuItem reDo; // redo menu item
    @FXML
    private MenuItem copy; // copy menu item
    @FXML
    private MenuItem cut; // cut menu item
    @FXML
    private MenuItem unDo; // undo menu item
    @FXML
    private MenuItem paste; // paste menu item
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private StyleClassedTextArea console;

    /**
     * Constructor method of the class {@code Controller}
     * <p>
     * Set number of UntitledTabs to 0
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    public Controller() {

        // Initializes all fields
        this.savedTabsHashMap = new HashMap<>();
        this.tabFileHashMap = new HashMap<>();
        this.executor = Executors.newSingleThreadExecutor();
        this.currentStage = null;

    }


    /**
     * Initializes the bindings to menu items
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    public void initialize(){
        // bindings if there is no tab open in the stage
        final BooleanBinding tabEmpty = Bindings.isEmpty(tabPane.getTabs());

        // bind the menu items to the property
        close.disableProperty().bind(tabEmpty);
        save.disableProperty().bind(tabEmpty);
        saveAs.disableProperty().bind(tabEmpty);
        selectAll.disableProperty().bind(tabEmpty);
        reDo.disableProperty().bind(tabEmpty);
        cut.disableProperty().bind(tabEmpty);
        copy.disableProperty().bind(tabEmpty);
        unDo.disableProperty().bind(tabEmpty);
        paste.disableProperty().bind(tabEmpty);
    }


    /**
     *  Method is called by main on project start.
     *
     *  Sets behavior of top left "exit" button to call "handleExitMenuItem" method.
     *
     * Opens initial tab at application start.
     *
     * @param (newStage) (A stage object passed to Controller from Main stored in controller)
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     * @see #handleNewMenuItem
     */
    @FXML
    public void setStage(Stage newStage){

        // Sets currentStage field
        this.currentStage = newStage;

        /* Trigger close menu item handler when tab is closed, consumes event
           Inelegancy : we consumed the event which could lead to problems down the line */
        this.currentStage.setOnCloseRequest((WindowEvent we) -> {
            we.consume();
            handleExitMenuItem();
        });

        // Opens initial tab
        handleNewMenuItem();



    }


    /**
     * Runs dialog popup when Hello is clicked.
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleHello() {

        // Creates inputDialog for user input that replaces button text
        TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.setTitle("Give me a number");
        inputDialog.setHeaderText("Give me an integer from 0 to 255");
        Optional<String> result = inputDialog.showAndWait();
        if(result.isPresent()){
            Hello.setText(inputDialog.getEditor().getText());
        }
    }

    /**
     * Handles response to Goodbye clicks.
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleGoodbye() {

        // Appends "Goodbye" to codeArea
        getCurrentCodeArea().appendText("Goodbye\n");
    }


    /**
     * Handler method that opens the About Window which contains application
     * information when user clicks "About..." MenuItem from the "File" MenuBar
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleAboutMenuItem() {

        // Displays alert dialog box containing application information
        Alert aboutDialogBox = new Alert(AlertType.INFORMATION);

        aboutDialogBox.setTitle("About");
        aboutDialogBox.setHeaderText("About this Application");

        aboutDialogBox.setContentText(
                "Authors: Dylan Tymkiw, Alex Yu, Jasper Loverude"
                        + "\nLast Modified: Feb 28, 2022");

        aboutDialogBox.show();
    }


    /**
     * Handler method that creates another tab on the tabPane, default name of the
     * newTab is generated by the helper method {@code getNewTabName}.
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     * @see #makeNewTab(String) #getNewTabName
     */
    @FXML
    private void handleNewMenuItem() {

        /* Calls makeNewTab, which makes and adds a new tab, and passes
        *  output from getNewTabName(), which is a string into it    */
        makeNewTab(getNewTabName());

    }


     /**
     * Handler method that exits the window when user clicks "Exit in the "File"
     * menu bar
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleExitMenuItem() {

        // If there are no tabs, ends thread and exits
        if(this.tabPane.getTabs().size() == 0) {
            this.setStyle(new CodeArea()).unsubscribe();
            System.exit(0);
        }

        // If there are tabs, iterates through them and checks if tabs need saving
        for (Tab tab : this.tabPane.getTabs()) {

            tabPane.getSelectionModel().select(tab);
            CodeArea codeArea = getCurrentCodeArea();

            String savedContent = savedTabsHashMap.get(tab);

            // the files have been saved before and has not changed since
            if (savedContent != null && savedContent.equals(codeArea.getText())) {
                continue;
            }
            // the file has never been saved but is empty
            else if (savedContent == null && codeArea.getText().isEmpty()){
                continue;
            }
            if(!helpClose(tab)) return;
        }

        // Stop threads and close program
        this.setStyle(new CodeArea()).unsubscribe();
        executor.shutdownNow();
        System.exit(0);


    }


    /**
     * Handler method that undoes the textarea edition
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleUndoMenuItem() {
        // Get current selected TextArea and undo its last edition
        CodeArea codeArea = getCurrentCodeArea();
        codeArea.undo();
    }


    /**
     * Handler method that redoes the textarea edition
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleRedoMenuItem() {
        // Get current selected TextArea and redo its last edition
        CodeArea codeArea = getCurrentCodeArea();
        codeArea.redo();
    }


    /**
     * Handler method that cuts the selected range of text and remove selection
     * Selected text will be copied to clipboard
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleCutMenuItem() {
        // Get current selected TextArea and cut its selected text
        CodeArea codeArea = getCurrentCodeArea();
        codeArea.cut();
    }


    /**
     * Handler method that copies the selected range of text and leaving selection
     * Selected text will be copied to clipboard
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleCopyMenuItem() {
        // Get current selected TextArea and copy its selected text
        CodeArea codeArea = getCurrentCodeArea();
        codeArea.copy();
    }


    /**
     * Handler method that inserts current clipboard content, or replacing selected
     * text
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handlePasteMenuItem() {
        // Get current selected TextArea and paste text from clipboard
        CodeArea codeArea = getCurrentCodeArea();
        codeArea.paste();
    }


    /**
     * Handler method that selects all text in the current TextArea
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleSelectAllMenuItem() {
        // Get current selected TextArea and paste text from clipboard
        CodeArea codeArea = getCurrentCodeArea();
        codeArea.selectAll();
    }


    /**
     * Handler method that saves the contents of the current text area to a file
     * when user clicks "Save As" menu bar
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleSaveAsMenuItem() {

        // get the current tab
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        // get the current textBox
        CodeArea codeBox = getCurrentCodeArea();

        // initiate a new file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as");

        //Set extension filter
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("fxml, css, java files", "*.java", "*.css", "*.fxml");//
        fileChooser.getExtensionFilters().addAll(extFilter);

        //Show save file dialog
        File file = fileChooser.showSaveDialog(tabPane.getScene().getWindow());

        if (file != null) {
            Alert alert;
            if (saveFile(currentTab, codeBox.getText(), file)) {

                alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Successfully created " + file.getPath());
                alert.show();
                // change the name of the tab to the file path
                String[] fileAncestors = file.getPath().split("/");
                currentTab.setText(fileAncestors[fileAncestors.length - 1]);
                currentTab.setId(file.getPath());
                //Add file to hashMap
                this.tabFileHashMap.put(currentTab,file);

            } else {
                alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed creating " + file.getPath());
                alert.show();
            }
        }
    }


    /**
     * Handler method that opens a text file and reads it into a text area of a new
     * tab
     *
     * Throws IOException if file does not exist.
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleOpenMenuItem() throws IOException {
        // creates a file chooser dialog so that user can choose which file to open
        FileChooser fileChooser = new FileChooser();

        // set extension filer for text files
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("java, fxml, css files", "*.java", "*.fxml", "*.css"));

        // check if user selected a file; if not, exit the method
        File selectedFile = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        // creates a new tab with filename as its title
        Tab newTab = makeNewTab(selectedFile.getName());

        // creates a new text area and loads the contents of the text file into it
        CodeArea newCodeArea = new CodeArea();// think about how to implement this
        this.setStyle(newCodeArea);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                newCodeArea.appendText(nextLine + "\n");
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        newTab.setContent(new VirtualizedScrollPane<>(newCodeArea));

        /* Puts tab into tabFileHashmap, savedTabsHashMap (so that it is not accidentally
        overwritten, and finally adds to the tabPane*/
        this.tabFileHashMap.put(newTab,selectedFile);
        this.savedTabsHashMap.put(newTab, newCodeArea.getText());
        this.tabPane.getSelectionModel().select(newTab);

    }


    /**
     * Handler method that saves the contents of a text area
     * into a file if it already exists.
     * If not, behaves like "Save as..."
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleSaveMenuItem() {
        // get the current tab
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();

        // get the file of current tab
        File file = this.tabFileHashMap.get(currentTab);
        if (file != null) {
            if (file.exists()) {
                // get content of textarea
                CodeArea codeBox = getCurrentCodeArea();
                String content = codeBox.getText();

                // save the content of the current tab
                saveFile(currentTab, content, file);

            } else {
                handleSaveAsMenuItem();
            }
        }else{
            handleSaveAsMenuItem();
        }
    }


    /**
     * Handler method that closes the tab differently based on the following
     * conditions.
     * If it's been changed since the last saving, asks user for whether to save.
     * If not, simply close the current tab.
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    @FXML
    private void handleCloseMenuItem() {
        // check if current area has changed
        CodeArea codeArea = getCurrentCodeArea();
        Tab selectedTab = this.tabPane.getSelectionModel().getSelectedItem();
        String savedContent = this.savedTabsHashMap.remove(selectedTab);
        if (savedContent == null && codeArea.getText().equals("")) {
            this.tabPane.getTabs().remove(selectedTab);
            return;
        }
        else if (savedContent != null && savedContent.equals(codeArea.getText())){
            this.tabPane.getTabs().remove(selectedTab);
            return;
        }
        if (!this.helpClose(selectedTab)) return;
        this.tabPane.getTabs().remove(selectedTab);
    }


    /**
     * Helper method for creating a new file
     *
     * @param (content) (the string content of the new file being created)
     * @param (file)    (the file variable passed by handleSaveAsMenuItem function indicating the
     *                  file the user want to save to is valid)
     * @param (tab)    (the tab passed by handleSaveAsMenuItem / handleSaveMenuItem indicating the
     *                  tab saved, for hashing into savedTabs HashMap)
     *
     * @return returns true if file created successfully and false if error occurs
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    private boolean saveFile(Tab tab, String content, File file) {

        try {
            FileWriter fileWriter;
            fileWriter = new FileWriter(file);
            fileWriter.write(content);
            fileWriter.close();
            this.savedTabsHashMap.put(tab, content);
            return true;
        } catch (IOException ex) {
            return false;
        }

    }


    /**
     * Helper method that is responsible for creating new tab on the
     * tabPane, and attaching close behavior to handleCloseMenuItem
     *
     * @param (tabTitle) (String parameter for the tabTile that sets text of tab)
     * @author Dylan Tymkiw, Alex Yu, Jasp@er Loverude
     * @see #getNewTabName
     */
    @FXML
    private Tab makeNewTab(String tabTitle){

        // Creates new tab
        Tab newTab = new Tab();
        newTab.setText(tabTitle);
        tabPane.getTabs().add(newTab);

        CodeArea newCodeArea = new CodeArea();
        this.setStyle(newCodeArea);

        VirtualizedScrollPane newPane = new VirtualizedScrollPane(newCodeArea);

        // Attaches contents to tab and tab to tabpane
        newTab.setContent(newPane);
        tabPane.getSelectionModel().select(newTab);

        /* Sets tab behavior on close request
           In-elegancy: consumes event t       */
        newTab.setOnCloseRequest((Event t) -> {
            t.consume();
            handleCloseMenuItem();
        });

        // Returns tab object
        return newTab;

    }


    /**
     * Helper method that generates the name for a new created tab,
     * increment the total number of untitled tabs and generate the new name,
     * returns the next "Untitled-x" with lowest possible x, or returns
     * "Untitled" if it is not currently taken.
     *
     * @return String name for newTab
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    private String getNewTabName() {

        // Stores whether "Untitled" is currently in use, iterates through all tabs
        boolean hasDefaultUntitled = false;

        for(Tab t : this.tabPane.getTabs())
            if(t.getText().equals("Untitled")) hasDefaultUntitled = true;

        // If "Untitled" not in use, returns it
        if(!hasDefaultUntitled) return "Untitled";

        // Iterates through every tab in tabPane, until the lowest "Untitled-x" is found
        int untitledNumber = 1;
        String nextUntitledName = "Untitled-" + untitledNumber;

        for(int i = 0; i < this.tabPane.getTabs().size(); i++) {
            for (Tab t : this.tabPane.getTabs()) {
                if (nextUntitledName.equals(t.getText())) {
                    nextUntitledName = "Untitled-" + untitledNumber;
                    untitledNumber++;
                }
            }
        }

        // Returns "Untitled-x"
        return nextUntitledName;
    }


    /**
     * Helper method that returns the current selected TextArea in the current tab
     *
     * @return Current selected TextArea
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    private CodeArea getCurrentCodeArea() {

        // Gets current code area and returns it
        VirtualizedScrollPane pane = (VirtualizedScrollPane)
                tabPane.getSelectionModel().getSelectedItem().getContent();
        return (CodeArea) pane.getContent();
    }


    /**
     * Helper method for creating the highlighting style for the text in the code area
     *
     * @param (text) (the string content of the new file being created)
     * @return returns a task that is a StyleSpan of collection of strings
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    private static StyleSpans<Collection<String>> computeHighlighting(String text) {

        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("PAREN") != null ? "paren" :
                    matcher.group("BRACE") != null ? "brace" :
                    matcher.group("BRACKET") != null ? "bracket" :
                    matcher.group("SEMICOLON") != null ? "semicolon" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("COMMENT") != null ? "comment" :
                    matcher.group("INT") != null ? "int":
                    null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }


    /**
     * Helper method that creates a task that is used to highlight the text
     * @return a task
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     * */
    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = getCurrentCodeArea().getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                return computeHighlighting(text);
            }
        };
        executor.execute(task);
        return task;
    }


    /**
     * Helper method for applying highlighting
     * @param (highlighting) (the )
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     * */
    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        getCurrentCodeArea().setStyleSpans(0, highlighting);
    }


    /**
     * Helper method to set the style of the text in code area
     * @param (codeArea) (the code area that needs to be styled)
     * @return a subscription object that needs to be stopped before
     * the program is finished
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     * */
    private Subscription setStyle(CodeArea codeArea){
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        Subscription cleanupWhenDone = codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(100))
                .retainLatestUntilLater(executor)
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(codeArea.multiPlainChanges())
                .filterMap(t -> {
                    if(t.isSuccess()) {
                        return Optional.of(t.get());
                    } else {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                })
                .subscribe(this::applyHighlighting);
        return cleanupWhenDone;
    }


    /**
     * Helper method for closing tabs
     *
     * @author Dylan Tymkiw, Alex Yu, Jasper Loverude
     */
    private boolean helpClose(Tab tab){
        Alert exitDialogue = new Alert(AlertType.CONFIRMATION,
                "Would you like to save: " + tab.getText(),
                ButtonType.NO, ButtonType.YES, ButtonType.CANCEL);
        Optional<ButtonType> result = exitDialogue.showAndWait();
        if (result.get() == ButtonType.YES) {
            handleSaveMenuItem();
        } else if (result.get() == ButtonType.CANCEL) {
            return false;
        }
        return true;
    }
}
