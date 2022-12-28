package org.qp.android.viewModel.viewModels;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.ACTION_OPEN_DOCUMENT_TREE;
import static android.content.Intent.EXTRA_MIME_TYPES;
import static org.qp.android.utils.FileUtil.GAME_INFO_FILENAME;
import static org.qp.android.utils.FileUtil.copyFile;
import static org.qp.android.utils.FileUtil.createFile;
import static org.qp.android.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.utils.FileUtil.getOrCreateDirectory;
import static org.qp.android.utils.FileUtil.isWritableDirectory;
import static org.qp.android.utils.FileUtil.isWritableFile;
import static org.qp.android.utils.PathUtil.normalizeFolderName;
import static org.qp.android.utils.PathUtil.removeExtension;
import static org.qp.android.utils.XmlUtil.objectToXml;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.qp.android.R;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.databinding.DialogInstallBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.model.install.InstallException;
import org.qp.android.model.install.Installer;
import org.qp.android.model.notify.NotifyBuilder;
import org.qp.android.utils.ArchiveUtil;
import org.qp.android.view.game.GameActivity;
import org.qp.android.view.stock.StockActivity;
import org.qp.android.view.stock.dialogs.StockDialogFrags;
import org.qp.android.view.stock.dialogs.StockDialogType;
import org.qp.android.viewModel.repository.LocalGame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ActivityStock extends AndroidViewModel {
    private final String TAG = this.getClass().getSimpleName();

    public ObservableField<StockActivity> activityObservableField = new
            ObservableField<>();
    public ObservableBoolean isShowDialog = new ObservableBoolean();

    private final LocalGame localGame = new LocalGame();
    private final HashMap<String, GameData> gamesMap = new HashMap<>();

    private File gamesDir;
    private DocumentFile tempInstallFile, tempInstallDir, tempImageFile, tempPathFile;

    private DialogInstallBinding installBinding;
    private GameData tempGameData;
    private DialogEditBinding editBinding;

    public MutableLiveData<Integer> outputIntObserver = new MutableLiveData<>();

    // region Getter/Setter
    public GameData getTempGameData() {
        return tempGameData;
    }

    public void setTempGameData(GameData tempGameData) {
        this.tempGameData = tempGameData;
    }

    public void setTempPathFile(DocumentFile tempPathFile) {
        this.tempPathFile = tempPathFile;
        editBinding.fileTV.setText(tempPathFile.getName());
    }

    public void setTempInstallFile(@NonNull DocumentFile tempInstallFile) {
        this.tempInstallFile = tempInstallFile;
        installBinding.fileTV.setText(tempInstallFile.getName());
    }

    public void setTempInstallDir (@NonNull DocumentFile tempInstallDir) {
        this.tempInstallDir = tempInstallDir;
        installBinding.buttonSelectArchive.setEnabled(false);
        installBinding.folderTV.setText(tempInstallDir.getName());
    }

    public void setTempImageFile(@NonNull DocumentFile tempImageFile) {
        this.tempImageFile = tempImageFile;
    }

    public void setGamesDir(File gamesDir) {
        this.gamesDir = gamesDir;
    }

    public HashMap<String, GameData> getGamesMap() {
        return gamesMap;
    }
    // endregion Getter/Setter

    public ActivityStock(@NonNull Application application) {
        super(application);
    }

    // region Dialog
    private StockDialogFrags dialogFragments = new StockDialogFrags();

    public void showDialogInstall() {
       dialogFragments.setDialogType(StockDialogType.INSTALL_DIALOG);
       dialogFragments.setInstallBinding(formingInstallView());
       dialogFragments.onCancel(new DialogInterface() {
            @Override
            public void cancel() {
                isShowDialog.set(false);
            }

            @Override
            public void dismiss() {
            }
        });
       Objects.requireNonNull(activityObservableField.get())
               .showInstallDialogFragment(dialogFragments);
       isShowDialog.set(true);
    }

    public void createInstallIntent() {
        GameData gameData = new GameData();
        try {
            gameData.id = removeExtension(Objects.requireNonNull(tempInstallFile != null ?
                    tempInstallFile.getName()
                    : tempInstallDir.getName()));
            gameData.title = (Objects.requireNonNull(
                    installBinding.ET0.getEditText()).getText().toString().isEmpty()?
                    removeExtension(Objects.requireNonNull(tempInstallFile != null ?
                            tempInstallFile.getName() :
                            tempInstallDir.getName()))
                    : Objects.requireNonNull(
                            installBinding.ET0.getEditText()).getText().toString());
            gameData.author = (Objects.requireNonNull(
                    installBinding.ET1.getEditText()).getText().toString().isEmpty()?
                    null
                    : Objects.requireNonNull(
                            installBinding.ET1.getEditText()).getText().toString());
            gameData.version = (Objects.requireNonNull(
                    installBinding.ET2.getEditText()).getText().toString().isEmpty()?
                    null
                    : Objects.requireNonNull(
                            installBinding.ET2.getEditText()).getText().toString());
            gameData.fileSize = String.valueOf(tempInstallFile != null ?
                    tempInstallFile.length()
                    : tempInstallDir.length() / 1000);
            gameData.icon = (tempImageFile == null ? null : tempImageFile.getUri().toString());
            installGame(tempInstallFile != null ? tempInstallFile : tempInstallDir, gameData);
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            Log.e(TAG, "Error: ", ex);
        }
    }

    public void showDialogEdit () {
        dialogFragments = new StockDialogFrags();
        dialogFragments.setDialogType(StockDialogType.EDIT_DIALOG);
        dialogFragments.setEditBinding(formingEditView());
        dialogFragments.onCancel(new DialogInterface() {
            @Override
            public void cancel() {
                isShowDialog.set(false);
            }

            @Override
            public void dismiss() {
            }
        });
        Objects.requireNonNull(activityObservableField.get())
                .showEditDialogFragment(dialogFragments);
        isShowDialog.set(true);
    }

    public void playGame() {
        var intent = new Intent(activityObservableField.get(),
                GameActivity.class);
        intent.putExtra("gameId", tempGameData.id);
        intent.putExtra("gameTitle", tempGameData.title);
        intent.putExtra("gameDirUri", tempGameData.gameDir.getAbsolutePath());

        var gameFileCount = tempGameData.gameFiles.size();
        if (gameFileCount == 0) {
            Log.w(TAG, "GameData has no gameData files");
            return;
        }

        if (gameFileCount == 1) {
            intent.putExtra("gameFileUri", tempGameData.gameFiles.get(0).getAbsolutePath());
            Objects.requireNonNull(activityObservableField.get()).startActivity(intent);
        } else {
            if (outputIntObserver.hasObservers()) {
                outputIntObserver = new MutableLiveData<>();
            }

            ArrayList<String> names = new ArrayList<>();
            for (File file : tempGameData.gameFiles) {
                names.add(file.getName());
            }

            var dialogFragments = new StockDialogFrags();
            dialogFragments.setDialogType(StockDialogType.SELECT_DIALOG);
            dialogFragments.setNames(names);
            dialogFragments.setCancelable(false);
            Objects.requireNonNull(activityObservableField.get())
                    .showSelectDialogFragment(dialogFragments);

            outputIntObserver.observeForever(integer -> {
                intent.putExtra("gameFileUri",
                        tempGameData.gameFiles.get(integer).getAbsolutePath());
                Objects.requireNonNull(activityObservableField.get()).startActivity(intent);
            });
        }
    }

    public void createEditIntent() {
        try {
            tempGameData.title = (Objects.requireNonNull(
                    editBinding.ET0.getEditText()).getText().toString().isEmpty()?
                    removeExtension(tempGameData.title)
                    : Objects.requireNonNull(
                    editBinding.ET0.getEditText()).getText().toString());
            tempGameData.author = (Objects.requireNonNull(
                    editBinding.ET1.getEditText()).getText().toString().isEmpty()?
                    removeExtension(tempGameData.author)
                    : Objects.requireNonNull(
                    editBinding.ET1.getEditText()).getText().toString());
            tempGameData.version = (Objects.requireNonNull(
                    editBinding.ET2.getEditText()).getText().toString().isEmpty()?
                    removeExtension(tempGameData.version)
                    : Objects.requireNonNull(
                    editBinding.ET2.getEditText()).getText().toString());
            tempGameData.icon = (tempImageFile == null ? tempGameData.icon
                    : tempImageFile.getUri().toString());
            writeGameInfo(tempGameData, tempGameData.gameDir);
            if (tempPathFile != null) {
                copyFile(activityObservableField.get(), tempPathFile, tempGameData.gameDir);
            }
            refreshGamesDirectory();
            isShowDialog.set(false);
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            Log.e(TAG, "Error: ", ex);
        }
    }

    public void sendIntent(@NonNull View view) {
        String action;
        Intent intentInstall, intentInstallDir, intentGetImage, intentSetPath;
        int id = view.getId();
        if (id == R.id.buttonSelectArchive) {
            action = ACTION_OPEN_DOCUMENT;
            intentInstall = new Intent(action);
            intentInstall.addCategory(Intent.CATEGORY_OPENABLE);
            intentInstall.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentInstall.setType("*/*");
            String[] mimeTypes = {"application/zip", "application/rar"};
            intentInstall.putExtra(EXTRA_MIME_TYPES, mimeTypes);
            try {
                Objects.requireNonNull(activityObservableField.get())
                        .resultInstallLauncher.launch(
                                Intent.createChooser(intentInstall, "Select an archive"));
            } catch (ActivityNotFoundException e) {
                Log.e(TAG , e.toString());
            }
        } else if (id == R.id.buttonSelectFolder) {
            action = ACTION_OPEN_DOCUMENT_TREE;
            intentInstallDir = new Intent(action);
            intentInstallDir.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                Objects.requireNonNull(activityObservableField.get())
                        .resultInstallDir.launch(intentInstallDir);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG , e.toString());
            }
        } else if (id == R.id.buttonSelectIcon) {
            action = ACTION_OPEN_DOCUMENT;
            intentGetImage = new Intent(action);
            intentGetImage.addCategory(Intent.CATEGORY_OPENABLE);
            intentGetImage.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentGetImage.setType("*/*");
            String[] mimeTypes = {"image/png", "image/jpeg"};
            intentGetImage.putExtra(EXTRA_MIME_TYPES, mimeTypes);
            try {
                Objects.requireNonNull(activityObservableField.get())
                        .resultGetImageLauncher.launch(
                                Intent.createChooser(intentGetImage, "Select an image"));
            } catch (ActivityNotFoundException e) {
                Log.e(TAG , e.toString());
            }
        } else if (id == R.id.buttonSelectPath) {
            action = ACTION_OPEN_DOCUMENT;
            intentSetPath = new Intent(action);
            intentSetPath.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentSetPath.setType("application/octet-stream");
            try {
                Objects.requireNonNull(activityObservableField.get())
                        .resultSetPath.launch(intentSetPath);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG , e.toString());
            }
        }
    }

    @NonNull
    private DialogInstallBinding formingInstallView() {
        installBinding =
                DialogInstallBinding.inflate(LayoutInflater.from(activityObservableField.get()));
        installBinding.setStockVM(this);
        return installBinding;
    }

    @NonNull
    private DialogEditBinding formingEditView() {
        editBinding =
                DialogEditBinding.inflate(LayoutInflater.from(activityObservableField.get()));
        editBinding.setStockVM(this);
        return editBinding;
    }
    // endregion Dialog

    // region Game install
    @NonNull
    public File getOrCreateGameDirectory(String gameName) {
        var folderName = normalizeFolderName(gameName);
        return getOrCreateDirectory(gamesDir, folderName);
    }

    public void installGame(DocumentFile gameFile, GameData gameData) {
        if (!isWritableDirectory(gamesDir)) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("Games directory is not writable");
            return;
        }
        try {
            doInstallGame(gameFile, gameData);
        } catch (InstallException ex) {
            if (Objects.requireNonNull(ex.getMessage()).equals("NIG")) {
                String message = getApplication()
                        .getString(R.string.installError)
                        .replace("-GAMENAME-", gameData.title);
                Objects.requireNonNull(activityObservableField.get()).showErrorDialog(message);
            } else if (ex.getMessage().equals("NFE")) {
                String message = getApplication()
                        .getString(R.string.noGameFilesError);
                Objects.requireNonNull(activityObservableField.get()).showErrorDialog(message);
            }
        }
    }

    private void doInstallGame(DocumentFile gameFile, GameData gameData) {
        var gameDir = getOrCreateGameDirectory(gameData.title);
        if (!isWritableDirectory(gameDir)) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("Games directory is not writable");
            return;
        }
        if (isWritableDirectory(gameDir)) {
            var installer = new Installer(activityObservableField.get());
            installer.gameInstall(gameFile, gameDir).observeForever(aBoolean -> {
                if (aBoolean) {
                    writeGameInfo(gameData , gameDir);
                    refreshGames();
                }
            });
            isShowDialog.set(false);
        } else {
            notificationStateGame(gameData.title);
            var installer = new Installer(activityObservableField.get());
            installer.gameInstall(gameFile, gameDir).observeForever(aBoolean -> {
                if (aBoolean) {
                    writeGameInfo(gameData , gameDir);
                    refreshGames();
                }
            });
            isShowDialog.set(false);
        }
    }

    private void notificationStateGame (String gameName) {
        var builder =
                new NotifyBuilder(activityObservableField.get(),"gameInstallationProgress");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.createDefaultChannel();
            builder.createProgressChannel();
        }
        builder.setTitleNotify(getApplication().getString(R.string.titleNotify));
        builder.setTextNotify(getApplication().getString(R.string.textProgressNotify));
        var notificationManager = NotificationManagerCompat.from(getApplication());
        notificationManager.notify(1, builder.buildWithProgress());
        ArchiveUtil.progressInstall.observeForever(aLong -> {
            notificationManager
                    .notify(1, builder.updateProgress((int) (aLong * 100 / ArchiveUtil.totalSize)));
            if (aLong == ArchiveUtil.totalSize) {
                Log.d(TAG, "this");
                notificationManager.cancelAll();
                var notifyBuilder =
                        new NotifyBuilder(activityObservableField.get(), "gameInstalled");
                notifyBuilder.setTitleNotify(getApplication().getString(R.string.titleNotify));
                var tempMessage = getApplication()
                        .getString(R.string.textInstallNotify)
                        .replace("-GAMENAME-", gameName);
                notifyBuilder.setTextNotify(tempMessage);
                notificationManager.notify(1, notifyBuilder.buildWithoutProgress());
            }
        });
    }

    public void writeGameInfo(GameData gameData , File gameDir) {
        var infoFile = findFileOrDirectory(gameDir, GAME_INFO_FILENAME);
        if (infoFile == null) {
            infoFile = createFile(gameDir, GAME_INFO_FILENAME);
        }
        if (!isWritableFile(infoFile)) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("Game data info file is not writable");
            return;
        }
        try (var out = new FileOutputStream(infoFile);
             var writer = new OutputStreamWriter(out)) {
            writer.write(objectToXml(gameData));
        } catch (Exception ex) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("Failed to write to a gameData info file");
        }
    }
    // endregion Game install

    // region Refresh
    public void refreshGamesDirectory() {
        var extFilesDir = getApplication().getExternalFilesDir(null);
        if (extFilesDir == null) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("External files directory not found");
            return;
        }
        var dir = getOrCreateDirectory(extFilesDir, "games");
        if (!isWritableDirectory(dir)) {
            var message = "Games directory is not writable" + " " +
                    getApplication().getString(R.string.gamesDirError);
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog(message);
            return;
        }
        setGamesDir(dir);
        refreshGames();
    }

    public void refreshGames() {
        gamesMap.clear();
        for (GameData localGameData : localGame.getGames(gamesDir)) {
            var remoteGameData = gamesMap.get(localGameData.id);
            if (remoteGameData != null) {
                var aggregateGameData = new GameData(remoteGameData);
                aggregateGameData.gameDir = localGameData.gameDir;
                aggregateGameData.gameFiles = localGameData.gameFiles;
                gamesMap.put(localGameData.id, aggregateGameData);
            } else {
                gamesMap.put(localGameData.id, localGameData);
            }
        }
        Objects.requireNonNull(activityObservableField.get()).setRecyclerAdapter();
    }
    // endregion Refresh
}

