package sample;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private FileChooser fileChooser;
    private File excelOurFile;
    private File excelTheirFile;

    @FXML private Button choseOur;

    @FXML private ProgressBar progressCheck;

    @FXML private TextField fileOurTextField;

    @FXML private TextField fileTheirTextField;

    @FXML private Label statusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите фаил");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Excel files(*.xls, *.xlsx)", "*.xls; *.xlsx"));
    }

    @FXML
    public void openFileOurDialogButton(MouseEvent event){
        File file = setPathFileInTextField(((Node)event.getSource()).getScene(), fileOurTextField);
        if(file != null){
            excelOurFile = file;
        }
    }

    @FXML
    public void openFileTheirDialogButton(MouseEvent event){
        File file = setPathFileInTextField(((Node)event.getSource()).getScene(), fileTheirTextField);
        if(file != null){
            excelTheirFile = file;
        }
    }

    private File setPathFileInTextField(Scene scene, TextField textField){
        File file = fileChooser.showOpenDialog(scene.getWindow());
        if(file != null) {
            textField.setText(file.getAbsolutePath());
        }
        return file;
    }

    @FXML
    public void checkExcels(MouseEvent mouseEvent){
        if(fileOurTextField.getText() != null && fileTheirTextField.getText() != null) {
            System.out.println(excelOurFile.getAbsoluteFile());
            Task task = createTask();
            progressCheck.setProgress(0);
            progressCheck.progressProperty().unbind();
            progressCheck.progressProperty().bind(task.progressProperty());
            task.messageProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    statusLabel.setText("Статус: " + newValue);
                }
            });
            new Thread(task).start();
        }
    }

    public Task createTask(){
        return new Task() {
            @Override
            protected Object call() {
                updateMessage("start");
                ActOfReconciliation our = null, their = null;
                try {
                    FileInputStream excelOurFileStream = new FileInputStream(excelOurFile);
                    FileInputStream excelTheirFileStream = new FileInputStream(excelTheirFile);
                    our = new ActOfReconciliation(WorkbookFactory.create(excelOurFileStream), excelOurFile.getName());
                    their = new ActOfReconciliation(WorkbookFactory.create(excelTheirFileStream), excelTheirFile.getName());
                    updateMessage("Поиск колонок");
                    our.findTabs();
                    their.findTabs();
                    updateProgress(10, 100);
                    updateMessage("Колонки найдены. Поиск конца таблицы");
                    our.findEndOfTable();
                    their.findEndOfTable();
                    updateMessage("Найдено " + our.compare(their) + " различий.");
                    updateProgress(90, 100);
                    excelOurFileStream.close();
                    excelTheirFileStream.close();
                    FileOutputStream excelOurFileOutStream = new FileOutputStream(excelOurFile);
                    FileOutputStream excelTheirOutFileStream = new FileOutputStream(excelTheirFile);
                    our.save(excelOurFileOutStream);
                    their.save(excelTheirOutFileStream);
                    excelOurFileOutStream.close();
                    excelTheirOutFileStream.close();
                    updateProgress(100, 100);
                    System.out.println(our);
                    System.out.println(their);

                } catch (IOException e) {
                    e.printStackTrace();
                    updateMessage("exception");
                    return false;
                } catch (InvalidFormatException e) {
                    e.printStackTrace();
                    updateMessage("InvalidFormatException ");
                } catch (Exception e) {
                    e.printStackTrace();
                    updateMessage(e.getMessage());
                }
                if (our != null && their != null) {
                    our.close();
                    their.close();
                }

                return true;
            }
        };
    }

    private Workbook getWorkbook(FileInputStream file) throws IOException, InvalidFormatException {
        /*Workbook workbook;
        if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase(".xls"))
            workbook = new HSSFWorkbook(new FileInputStream(file));
        else
            workbook = new XSSFWorkbook(file);*/
        return WorkbookFactory.create(file);
    }
}
