import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.SocketHandler;

enum Shape{
    NONE,
    BLOCK,
    BEEHIVE,
    BLINKER,
    TOAD,
    GLIDER,
    FREE,
}

public class Main extends Application {
    Label status = new Label("Initialized");
    Label frame = new Label("Frame: 0");
    int canvasWidth = 1200;
    int canvasHeight = 800;

    // Use col as x, row as y. Consistent with canvas x y
    int boardRow = 50;
    int boardCol = 75;
    // To handle all edges, make it as big as possible, then we will need an offset
    boolean[][] displayingBoard = new boolean[boardCol][boardRow];
    boolean[][] calculatingBoard = new boolean[boardCol][boardRow];

    Shape chosenShape = Shape.NONE;

    final Canvas canvas = new Canvas(canvasWidth, canvasHeight);

    boolean onPause = false;

    int frameCount = 0;
    @Override
    public void start(Stage stage) throws Exception {

        AnchorPane root = new AnchorPane();

        // Set Bottom Status Tool Bar
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar bottomBar = new ToolBar();
        bottomBar.getItems().addAll(status, spacer, frame);
        AnchorPane.setBottomAnchor(bottomBar, 0.0);
        AnchorPane.setLeftAnchor(bottomBar, 0.0);
        AnchorPane.setRightAnchor(bottomBar, 0.0);

        root.getChildren().add(bottomBar);

        VBox vBox = new VBox();
        // Set Top Tool Bar
        setToolbar(vBox);

        // Draw Grids
        GraphicsContext gc = canvas.getGraphicsContext2D();
        canvas.setOnMouseClicked(event -> {
            canvasOnClick(event);
            drawBoard(gc);
            if (chosenShape != Shape.FREE){
                chosenShape = Shape.NONE;
            }
        });

        // Parameters of Grids are specially chosen for formatting purpose
        drawGrid(gc);

        vBox.setSpacing(6);
        vBox.getChildren().add(canvas);
        root.getChildren().add(vBox);
        AnchorPane.setTopAnchor(vBox, 0.0);
        AnchorPane.setLeftAnchor(vBox, 0.0);
        AnchorPane.setRightAnchor(vBox, 0.0);

        Scene scene = new Scene(root, 1200, 900);

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!onPause) {
                    refreshCell(gc);

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            frameCount++;
                            frame.setText(String.format("Frame: %d", frameCount));
                        }
                    });
                }
            }
        }, 0, 1000);

        // Stage Initialization
        stage.setTitle("Conway's Game of Life (y555zhao)");
        stage.setScene(scene);
        stage.setResizable(false);

        stage.setOnCloseRequest(e -> {
            timer.cancel();
            Platform.exit();
        });
        stage.show();
    }

    private void clear(GraphicsContext gc){
        for(int i = 0; i < boardCol; i++){
            for (int j = 0; j < boardRow; j++){
                setDisplayingBoard(i, j, false);
            }
        }

        refreshCell(gc);
        status.setText("Cleared");
    }

    private void canvasOnClick(MouseEvent event) {
        int x = (int) ((event.getSceneX() - canvas.getLayoutX())/ 16);
        int y = (int) ((event.getSceneY() - canvas.getLayoutY())/ 16);

        if (!inBorder(x, y)) return;

        switch (chosenShape) {
            case NONE:
                return;
            case BLOCK:
                setDisplayingBoard(x, y, true);
                setDisplayingBoard(x+1, y, true);
                setDisplayingBoard(x, y+1, true);
                setDisplayingBoard(x+1,y+1, true);
                status.setText(String.format("Created block at %d, %d", x+1, y+1));
                break;
            case BEEHIVE:
                setDisplayingBoard(x+1,y, true);
                setDisplayingBoard(x+2, y, true);
                setDisplayingBoard(x, y+1, true);
                setDisplayingBoard(x+3, y+1, true);
                setDisplayingBoard(x+1, y+2, true);
                setDisplayingBoard(x+2, y+2, true);
                status.setText(String.format("Created beehive at %d, %d", x+1, y+1));
                break;
            case BLINKER:
                setDisplayingBoard(x, y+1, true);
                setDisplayingBoard(x+1, y+1, true);
                setDisplayingBoard(x+2, y+1, true);
                status.setText(String.format("Created blinker at %d, %d", x+1, y+1));
                break;
            case TOAD:
                setDisplayingBoard(x+1, y, true);
                setDisplayingBoard(x+2, y, true);
                setDisplayingBoard(x+3, y, true);
                setDisplayingBoard(x, y+1, true);
                setDisplayingBoard(x+1, y+1, true);
                setDisplayingBoard(x+2, y+1, true);
                status.setText(String.format("Created toad at %d, %d", x+1, y+1));
                break;
            case GLIDER:
                setDisplayingBoard(x+2, y, true);
                setDisplayingBoard(x, y+1, true);
                setDisplayingBoard(x+2, y+1, true);
                setDisplayingBoard(x+1, y+2, true);
                setDisplayingBoard(x+2, y+2, true);
                status.setText(String.format("Created glider at %d, %d", x+1, y+1));
                break;
            case FREE:
                if (getDisplayingBoard(x, y)) {
                    status.setText(String.format("Killed cell at %d, %d", x+1, y+1));
                } else {
                    status.setText(String.format("Created cell at %d, %d", x+1, y+1));
                }
                setDisplayingBoard(x, y, !getDisplayingBoard(x, y));
        }

    }

    private void refreshCell(GraphicsContext gc){

        for(int i = 0; i <  boardCol; i++){
            for (int j = 0; j < boardRow; j++){
                int liveNeighbours = liveNeighborNum(i, j);

                // If the cell is live
                if (getDisplayingBoard(i, j)){
                    // With fewer than two or more than three live neighbours it dies
                    setCalculatingBoard(i, j, liveNeighbours >= 2 && liveNeighbours <= 3);
                } else {
                    setCalculatingBoard(i,j, liveNeighbours == 3);
                }
            }
        }

        copyArray(displayingBoard, calculatingBoard);
        drawBoard(gc);

    }

    private int liveNeighborNum(int x, int y){
        int counter = 0;


        for (int i = -1; i <= 1; i++){
            for (int j = -1; j <= 1; j++){
                // Skip itself
                if (i == 0 && j == 0){
                    continue;
                }

                // Still inside grid and get it
                if (getDisplayingBoard(x+i, y+j)){
                    counter ++;
                }
            }
        }

        return counter;
    }

    private void drawBoard(GraphicsContext gc) {
        for (int i = 0; i < boardCol; i++){
            for (int j = 0; j < boardRow; j++) {
                drawCell(gc, i, j, !getDisplayingBoard(i,j));
            }
        }
    }

    // @input: x, y means the position of the cell in the board 2d list
    private void drawCell(GraphicsContext gc, int x, int y, boolean clear) {
        // Each block would be 13*13 (Cuz I want to leave the gray line there.
        if (!inBorder(x, y)){
            return;
        }

        if (clear){
            gc.setFill(Color.WHITE);
        } else {
            gc.setFill(Color.BLACK);
        }

        // +2 for no-overlapping with grids and white border. Another +5 in y for our offsets
        gc.fillRect(16*x + 2, 16*y + 2, 13.0, 13.0);
    }

    private void drawGrid(GraphicsContext gc){

        // Draw grid
        gc.setStroke(Color.GRAY.brighter());
        gc.setLineWidth(1);

        // Horizontal
        for(int i = 0; i <= canvasHeight; i += 16){
            gc.moveTo(0, i);
            gc.lineTo(canvasWidth, i);
        }

        // Vertical
        for(int i = 0; i <= canvasWidth; i += 16){
            gc.moveTo(i, 0);
            gc.lineTo(i, canvasHeight);
        }

        gc.stroke();
    }

    private void setToolbar(VBox root) {
        ToggleButton buttonEdit = new ToggleButton();
        // Top tool bar
        // Top button Block
        // Create image
        Image imgBlock = new Image("block.png");
        ImageView viewBlock = new ImageView(imgBlock);
        viewBlock.setFitHeight(40);
        viewBlock.setFitWidth(40);
        // Create actual button
        Button buttonBlock = new Button();
        buttonBlock.setOnMouseClicked(event -> {
            chosenShape = Shape.BLOCK;
            buttonEdit.setSelected(false);
        });
        buttonBlock.setGraphic(viewBlock);
        buttonBlock.setText("Block");

        // Top button Beehive
        // Create image
        Image imgBeehive = new Image("beehive.png");
        ImageView viewBeehive = new ImageView(imgBeehive);
        viewBeehive.setFitHeight(40);
        viewBeehive.setFitWidth(52);
        // Create actual button
        Button buttonBeehive = new Button();
        buttonBeehive.setOnMouseClicked(event -> {
            chosenShape = Shape.BEEHIVE;
            buttonEdit.setSelected(false);
        });
        buttonBeehive.setGraphic(viewBeehive);
        buttonBeehive.setText("Beehive");

        // Top button Blinker
        // Create image
        Image imgBlinker = new Image("blinker.png");
        ImageView viewBlinker = new ImageView(imgBlinker);
        viewBlinker.setFitHeight(40);
        viewBlinker.setFitWidth(40);
        // Create actual button
        Button buttonBlinker = new Button();
        buttonBlinker.setOnMouseClicked(event -> {
            chosenShape = Shape.BLINKER;
            buttonEdit.setSelected(false);
        });
        buttonBlinker.setGraphic(viewBlinker);
        buttonBlinker.setText("Blinker");

        // Top button Toad
        // Create image
        Image imgToad = new Image("toad.png");
        ImageView viewToad = new ImageView(imgToad);
        viewToad.setFitHeight(40);
        viewToad.setFitWidth(80);
        // Create actual button
        Button buttonToad = new Button();
        buttonToad.setOnMouseClicked(event -> {
            chosenShape = Shape.TOAD;
            buttonEdit.setSelected(false);
        });
        buttonToad.setGraphic(viewToad);
        buttonToad.setText("Toad");

        // Top button Glider
        // Create image
        Image imgGlider = new Image("glider.png");
        ImageView viewGlider = new ImageView(imgGlider);
        viewGlider.setFitHeight(40);
        viewGlider.setFitWidth(40);
        // Create actual button
        Button buttonGlider = new Button();
        buttonGlider.setOnMouseClicked(event -> {
            chosenShape = Shape.GLIDER;
            buttonEdit.setSelected(false);
        });
        buttonGlider.setGraphic(viewGlider);
        buttonGlider.setText("Glider");

        // Top button Clear
        Image imgClear = new Image("clear.png");
        ImageView viewClear = new ImageView(imgClear);
        viewClear.setFitHeight(40);
        viewClear.setFitWidth(40);
        // Create actual button
        Button buttonClear = new Button();
        buttonClear.setOnMouseClicked(event -> {
            clear(canvas.getGraphicsContext2D());
        });
        buttonClear.setPrefSize(100, 40);
        buttonClear.setGraphic(viewClear);
        buttonClear.setText("Clear");

        // Top button Next Frame
        Image imgNext = new Image("next.png");
        ImageView viewNext = new ImageView(imgNext);
        viewNext.setFitHeight(40);
        viewNext.setFitWidth(40);
        Button buttonNext = new Button();
        buttonNext.setOnMouseClicked(event -> {
            frameCount ++;
            frame.setText(String.format("Frame: %d", frameCount));
            refreshCell(canvas.getGraphicsContext2D());
        });
        buttonNext.setText("Next Frame");
        buttonNext.setGraphic(viewNext);
        buttonNext.setDisable(true);

        // Top button Pause
        // Create image
        // Create actual button
        Image imgPause = new Image("pause.png");
        ImageView viewPause = new ImageView(imgPause);
        viewPause.setFitHeight(40);
        viewPause.setFitWidth(40);
        Button buttonPause = new Button();
        buttonPause.setOnMouseClicked(event -> {
            switchToPauseState(event, buttonPause, buttonNext, buttonEdit);
        });
        buttonPause.setPrefSize(110, 40);
        buttonPause.setGraphic(viewPause);
        buttonPause.setText("Pause");

        // Top button Edit
        // Create image
        // Create actual button
        Image imgEdit = new Image("edit.png");
        ImageView viewEdit = new ImageView(imgEdit);
        viewEdit.setFitHeight(40);
        viewEdit.setFitWidth(40);
        buttonEdit.setOnMouseClicked(event -> {
            // State change happens before onclicked
            if (!buttonEdit.isSelected()){
                chosenShape = Shape.NONE;
            } else  {
                switchToPauseState(event, buttonPause, buttonNext, buttonEdit);
                chosenShape = Shape.FREE;
            }
        });
        buttonEdit.setGraphic(viewEdit);
        buttonEdit.setText("Draw");


        // Create Tool bar
        ToolBar topToolBar = new ToolBar();
        topToolBar.getItems().addAll(buttonBlock, buttonBeehive, new Separator(),
                new Separator(), buttonBlinker, buttonToad, buttonGlider, new Separator(),
                new Separator(), buttonClear, buttonPause, buttonNext, buttonEdit);

        root.getChildren().add(topToolBar);

    }

    private void switchToPlayState(MouseEvent event, Button button, Button buttonNext, ToggleButton buttonEdit) {
        Image imgPause = new Image("pause.png");
        ImageView viewPause = new ImageView(imgPause);
        viewPause.setFitHeight(40);
        viewPause.setFitWidth(40);
        onPause = false;
        button.setText("Pause");
        button.setGraphic(viewPause);
        button.setOnMouseClicked(event1 -> {
            switchToPauseState(event1, button, buttonNext, buttonEdit);
        });
        buttonNext.setDisable(true);
        buttonEdit.setSelected(false);
        chosenShape = Shape.NONE;
    }

    private void switchToPauseState(MouseEvent event, Button button, Button buttonNext, ToggleButton buttonEdit){
        Image imgPlay = new Image("play.png");
        ImageView viewPlay = new ImageView(imgPlay);
        viewPlay.setFitHeight(40);
        viewPlay.setFitWidth(40);
        onPause = true;
        button.setText("Play");
        button.setGraphic(viewPlay);
        button.setOnMouseClicked(event1 -> {
        switchToPlayState(event1, button, buttonNext, buttonEdit);
    });
        buttonNext.setDisable(false);
        chosenShape = Shape.NONE;
    }

    private void copyArray(boolean[][] receiver, boolean[][] copyFrom){
        for(int i = 0; i < boardCol; i++){
            for (int j = 0; j < boardRow; j++){
                receiver[i][j] = copyFrom[i][j];
            }
        }
    }

    private void setDisplayingBoard(int x, int y, boolean status){
        if (!inBorder(x, y)) return;

        displayingBoard[x][y] = status;

    }

    private boolean getDisplayingBoard(int x, int y){
        if (!inBorder(x, y)) return false;

        return displayingBoard[x][y];
    }

    private void setCalculatingBoard(int x, int y, boolean status){
        if (!inBorder(x, y)) return;

        calculatingBoard[x][y] = status;
    }

    private boolean getCalculatingBoard(int x, int y){
        if (!inBorder(x, y)) return false;

        return calculatingBoard[x][y];
    }

    private boolean inBorder(int x, int y) {
        return x >= 0 && x < boardCol && y >= 0 && y < boardRow;
    }

    private void printBoard(int startx, int starty, int endx, int endy){

        for (int i = startx; i <= endx; i++){
            for (int j = starty; j <= endy; j++){
                System.out.printf("%s ", getDisplayingBoard(i, j));
            }
            System.out.print("\n");
        }
    }
}
