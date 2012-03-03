// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

// Copyright (c) 2012, Christoph Schmidt-Hieber

// Based on android-file-dialog
// http://code.google.com/p/android-file-dialog/
// alexander.ponomarev.1@gmail.com

package csh.neurodroid;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class FileDialog extends ListActivity {

    private static final String ITEM_KEY = "key";
    private static final String ITEM_IMAGE = "image";
    private static final String ITEM_CHECK = "check";
    private static final String ITEM_ENABLED = "enables";
    private static final String ITEM_FILE = "file";
    private static final String ROOT = "/";

    public static final String START_PATH = "START_PATH";
    public static final String RESULT_EXPORT_PATHS = "RESULT_EXPORT_PATHS";
    public static final String RESULT_OPEN_PATH = "RESULT_OPEN_PATH";
    public static final String RESULT_UPLOAD_PATH = "RESULT_UPLOAD_PATH";
    public static final String RESULT_SELECTED_FILE = "RESULT_SELECTED_FILE";
    public static final String SELECTION_MODE = "SELECTION_MODE";
    public static final String LABEL = "LABEL";
    public static final String BUTTON_LABEL = "BUTTON_LABEL";
    public static final String CURRENT_ROOT = "CURRENT_ROOT";
    public static final String CURRENT_ROOT_NAME = "CURRENT_ROOT_NAME";
    public static final String CURRENT_DBROOT = "CURRENT_DBROOT";

    private static final int NEW_FOLDER_DIALOG_ID = 0;
    
    private String currentRoot = ROOT;
    private String currentRootLabel = ROOT;
    private List<String> path = null;
    private TextView myPath;
    /* private EditText mFileName; */
    private ArrayList<HashMap<String, Object>> mList;

    private Button selectButton;

    private LinearLayout layoutSelect;
    private LinearLayout layoutUpload;
    private LinearLayout layoutCreate;
    
    private InputMethodManager inputManager;
    private String parentPath;
    private String currentPath = currentRoot;
    private String currentDBEncFS = "/";
    
    private File selectedFile;
    private int selectionMode = SelectionMode.MODE_OPEN;

    private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();

    private Set<String> selectedPaths = new HashSet<String>();
    
    /** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED, getIntent());

        setContentView(R.layout.file_dialog_main);
        myPath = (TextView) findViewById(R.id.path);
        /* mFileName = (EditText) findViewById(R.id.fdEditTextFile); */

        inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        selectionMode = getIntent().getIntExtra(SELECTION_MODE, SelectionMode.MODE_OPEN);

        currentRoot = getIntent().getStringExtra(CURRENT_ROOT);
        if (currentRoot == null) {
            currentRoot = ROOT;
        }
        currentPath = currentRoot;

        currentRootLabel = getIntent().getStringExtra(CURRENT_ROOT_NAME);
        if (currentRootLabel == null) {
            currentRootLabel = currentRoot;
        }

        currentDBEncFS = getIntent().getStringExtra(CURRENT_DBROOT); 
        
        String buttonLabel = getIntent().getStringExtra(BUTTON_LABEL);
        selectButton = (Button) findViewById(R.id.fdButtonSelect);
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN:
        case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
            selectButton.setEnabled(false);
            break;
        default:
            selectButton.setEnabled(true);
        }
        selectButton.setText(buttonLabel);
        selectButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    switch (selectionMode) {
                    case SelectionMode.MODE_OPEN:
                    case SelectionMode.MODE_OPEN_DB:
                    case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
                    case SelectionMode.MODE_OPEN_CREATE:
                    case SelectionMode.MODE_OPEN_CREATE_DB:
                        if (currentPath != null) {
                            getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
                            getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
                            getIntent().putExtra(RESULT_EXPORT_PATHS, currentPath);
                            if (selectedFile != null) {
                                getIntent().putExtra(RESULT_SELECTED_FILE, selectedFile.getPath());
                            } else {
                                getIntent().putExtra(RESULT_SELECTED_FILE, (String)null);
                            }
                            setResult(RESULT_OK, getIntent());
                            finish();
                        }
                        break;
                    default:
                    }
                }
            });

        layoutSelect = (LinearLayout) findViewById(R.id.fdLinearLayoutSelect);
        layoutCreate = (LinearLayout) findViewById(R.id.fdLinearLayoutCreate);
        layoutUpload = (LinearLayout) findViewById(R.id.fdLinearLayoutUpload);
        
        /* Disable upload at this time */ 
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN:
        case SelectionMode.MODE_OPEN_DB:
        case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
            layoutCreate.setVisibility(View.GONE);
            /* no break! */
        case SelectionMode.MODE_OPEN_CREATE:
        case SelectionMode.MODE_OPEN_CREATE_DB:
            layoutUpload.setVisibility(View.GONE);
            break;
        default:
            layoutCreate.setVisibility(View.GONE);
        }

        final Button cancelButton = (Button) findViewById(R.id.fdButtonCancel);
        cancelButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    setResult(RESULT_CANCELED, getIntent());
                    finish();
                }

            });

        final Button createButton = (Button) findViewById(R.id.fdButtonCreate);
        createButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                showDialog(NEW_FOLDER_DIALOG_ID);
            }

        });
        
        final Button uploadButton = (Button) findViewById(R.id.fdButtonUpload);
        uploadButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {

            }
            
        });

        final Button newFolderButton = (Button) findViewById(R.id.fdButtonNewFolder);
        newFolderButton.setOnClickListener(new OnClickListener() {
            
            public void onClick(View v) {
                showDialog(NEW_FOLDER_DIALOG_ID);
            }
        });
        
        String startPath = getIntent().getStringExtra(START_PATH);
        if (startPath != null) {
            getDir(startPath, currentRoot, currentRootLabel, currentDBEncFS);
        } else {
            getDir(currentRoot, currentRoot, currentRootLabel, currentDBEncFS);
        }
        String label = getIntent().getStringExtra(LABEL);
        this.setTitle(label);
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_MULTISELECT:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
            registerForContextMenu(getListView());
            break;
        }
    }

    private void getDir(String dirPath, String rootPath, String rootName, String dbRootPath) {

        boolean useAutoSelection = dirPath.length() < currentPath.length();

        Integer position = lastPositions.get(parentPath);
        
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_CREATE_DB:
        case SelectionMode.MODE_OPEN_DB:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
        case SelectionMode.MODE_OPEN_MULTISELECT:
        default:
            getDirImpl(dirPath, rootPath, rootName);
        }
        if (position != null && useAutoSelection) {
            getListView().setSelection(position);
        }

    }

    private void getDirImpl(final String dirPath, final String rootPath, final String rootName) {

        currentPath = dirPath;
        currentRoot = rootPath;
        currentRootLabel = rootName;
        String currentPathFromRoot = currentPath.substring(currentRoot.length());
        
        final List<String> item = new ArrayList<String>();
        path = new ArrayList<String>();
        mList = new ArrayList<HashMap<String, Object>>();
        
        File f = new File(currentPath);
        File[] files = f.listFiles();
        if (files == null) {
            currentPath = currentRoot;
            f = new File(currentPath);
            files = f.listFiles();
        }
        myPath.setText(getText(R.string.location) + ": " + currentRootLabel +
                       currentPathFromRoot);

        if (!currentPath.equals(currentRoot)) {

            item.add(currentRoot);
            addItem(new File(currentRoot), R.drawable.ic_launcher_folder, currentRootLabel);
            path.add(currentRoot);

            item.add("../");
            addItem(new File(f.getParent()), R.drawable.ic_launcher_folder, "../");
            path.add(f.getParent());
            parentPath = f.getParent();

        }

        TreeMap<String, String> dirsMap = new TreeMap<String, String>();
        TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
        TreeMap<String, String> filesMap = new TreeMap<String, String>();
        TreeMap<String, String> filesPathMap = new TreeMap<String, String>();

        /* getPath() returns full path including file name */
        for (File file : files) {
            if (file.isDirectory()) {
                String dirName = file.getName();
                dirsMap.put(dirName, dirName);
                dirsPathMap.put(dirName, file.getPath());
            } else {
                filesMap.put(file.getName(), file.getName());
                filesPathMap.put(file.getName(), file.getPath());
            }
        }

        item.addAll(dirsMap.tailMap("").values());
        item.addAll(filesMap.tailMap("").values());
        path.addAll(dirsPathMap.tailMap("").values());
        path.addAll(filesPathMap.tailMap("").values());

        for (String dirpath : dirsPathMap.tailMap("").keySet()) {
            addItem(new File(dirsPathMap.tailMap("").get(dirpath)),
                    R.drawable.ic_launcher_folder);
        }

        for (String filepath : filesPathMap.tailMap("").keySet()) {
            addItem(new File(filesPathMap.tailMap("").get(filepath)),
                    R.drawable.ic_launcher_file);
        }
        
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN:
        case SelectionMode.MODE_OPEN_DB:
        case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
        case SelectionMode.MODE_OPEN_CREATE:
        case SelectionMode.MODE_OPEN_CREATE_DB: {
            SimpleAdapter fileList = new SimpleAdapter(this, mList,
                    R.layout.file_dialog_row_single,
                    new String[] { ITEM_KEY, ITEM_IMAGE },
                    new int[] {R.id.fdrowtext, R.id.fdrowimage });
            fileList.notifyDataSetChanged();
            setListAdapter(fileList);
            break;
        }
        default: {
            ArrayAdapter<HashMap<String, Object>> fileList = 
                    new FileDialogArrayAdapter(this, mList);
            setListAdapter(fileList);
            getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            fileList.notifyDataSetChanged();
            setListAdapter(fileList);
        }
        }

    }

    private void addItem(File file, Integer imageId) {
        addItem(file, imageId, file.getName());
    }
    
    private void addItem(File file, Integer imageId, String filelabel) {
        HashMap<String, Object> item = new HashMap<String, Object>();
        
        item.put(ITEM_KEY, filelabel);
        item.put(ITEM_IMAGE, imageId);
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_MULTISELECT:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
            item.put(ITEM_CHECK, getChecked(file));
            item.put(ITEM_FILE, file);
            item.put(ITEM_ENABLED,
                     (Boolean) (!file.getPath().equals(currentRoot) &&
                                !file.getPath().equals(new File(currentPath).getParent())));
            break;
        }
        mList.add(item);
    }

    /** returns true if the parent directory is checked and/or
     *  the path itself is checked
     */
    private boolean getChecked(File file) {
        boolean allChildrenSelected = file.isDirectory();
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : file.listFiles()) {
                    if (!selectedPaths.contains(child.getPath())) {
                        allChildrenSelected = false;
                        break;
                    }
                }
                if (children.length == 0) {
                    allChildrenSelected = false;
                }
            } else {
                allChildrenSelected = false;
            }
        }
        if (selectedPaths.contains(file.getParent()) ||
            selectedPaths.contains(file.getPath()) ||
            allChildrenSelected) {
            selectedPaths.add(file.getPath());
            return true;
        } else {
            return false;
        }
    }
    
    static class ViewHolder {
        protected CheckBox checkbox;
        protected TextView text;
        protected ImageView image;
    }


    public class FileDialogArrayAdapter extends ArrayAdapter<HashMap<String, Object>> {

        private final List<HashMap<String, Object>> list;
        private final Activity context;

        public FileDialogArrayAdapter(Activity context, List<HashMap<String, Object>> list) {
            super(context, R.layout.file_dialog_row_multi, list);
            this.context = context;
            this.list = list;
        }
        
        @Override
            public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            if (convertView == null) {
                LayoutInflater inflator = context.getLayoutInflater();
                view = inflator.inflate(R.layout.file_dialog_row_multi, null);
                final ViewHolder viewHolder = new ViewHolder();
                viewHolder.image = (ImageView) view.findViewById(R.id.fdrowimage);
                viewHolder.text = (TextView) view.findViewById(R.id.fdrowtext);
                viewHolder.checkbox = (CheckBox) view.findViewById(R.id.fdrowcheck);
                viewHolder.checkbox
                    .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                            public void onCheckedChanged(CompoundButton buttonView,
                                                             boolean isChecked) {
                                @SuppressWarnings("unchecked")
                                HashMap<String, Object> element =
                                    (HashMap<String, Object>) viewHolder.checkbox
                                    .getTag();
                                element.put(ITEM_CHECK, buttonView.isChecked());
                                File f = (File) element.get(ITEM_FILE);
                                /* Avoid recursion for performance reasons */
                                if (buttonView.isChecked()) {
                                    selectedPaths.add(f.getPath());
                                } else {
                                    /* Is this a bug in the SDK ?? This will get fired
                                     * upon scrolling from disabled elements, which is why
                                     * we have to check whether the item is enabled.
                                     */
                                    if ((Boolean)element.get(ITEM_ENABLED)) {
                                        removePath(f);
                                    }
                                }
                            }
                        });
                view.setTag(viewHolder);
                viewHolder.checkbox.setTag(list.get(position));
            } else {
                view = convertView;
                ((ViewHolder) view.getTag()).checkbox.setTag(list.get(position));
            }
            ViewHolder holder = (ViewHolder) view.getTag();
            String filelabel = (String) list.get(position).get(ITEM_KEY);
            holder.text.setText(filelabel);
            Boolean enabled = (Boolean) list.get(position).get(ITEM_ENABLED);
            holder.checkbox.setEnabled(enabled);
            holder.checkbox.setChecked((Boolean) list.get(position).get(ITEM_CHECK) && enabled);
            holder.image.setImageResource((Integer) list.get(position).get(ITEM_IMAGE));
            return view;
        }
    }
    
    private void removePath(File f) {
        /* Remove all paths that have f as parent */
        if (f.isDirectory()) {
            Set<String> newSelectedPaths = new HashSet<String>();
            for (String path : selectedPaths) {
                if (path.indexOf(f.getPath()) == -1) {
                    newSelectedPaths.add(path);
                }
            }
            selectedPaths = newSelectedPaths;
        } else {
            selectedPaths.remove(f.getPath());
        }

        /* Remove all parent directories checkmarks */
        if (!f.getPath().equals(currentRoot)) {
            String parent = f.getParent();
            while (parent != null) {
                selectedPaths.remove(parent);
                if (parent.equals(currentRoot)) {
                    parent = null;
                } else {
                    parent = new File(parent).getParent();
                }
            }
        }
    }

    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

        File file = new File(path.get(position));

        setSelectVisible(v);

        if (file.isDirectory()) {
            switch (selectionMode) {
            case SelectionMode.MODE_OPEN:
            case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
                selectButton.setEnabled(false);
                break;
            default:
                selectButton.setEnabled(true);
            }

            if (file.canRead()) {
                lastPositions.put(currentPath, position);
                getDir(path.get(position), currentRoot, currentRootLabel, currentDBEncFS);
            } else {
                new AlertDialog.Builder(this)
                    .setIcon(R.drawable.icon)
                    .setTitle(
                                  "[" + file.getName() + "] "
                                  + getText(R.string.cant_read_folder))
                    .setPositiveButton("OK",
                                       new DialogInterface.OnClickListener() {
                                           
                                           public void onClick(DialogInterface dialog,
                                                               int which) {
                                               
                                           }
                                       }).show();
            }
        } else {
            selectButton.setEnabled(true);
            switch (selectionMode) {
            case SelectionMode.MODE_OPEN_MULTISELECT:
            case SelectionMode.MODE_OPEN_MULTISELECT_DB:
                break;
            default:
                selectedFile = file;
                v.setSelected(true);
            }
        }
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            selectButton.setEnabled(true);

            /* if (layoutCreate.getVisibility() == View.VISIBLE) {
                layoutCreate.setVisibility(View.GONE);
                layoutSelect.setVisibility(View.VISIBLE);
            } else {*/
                if (!currentPath.equals(currentRoot)) {
                    getDir(parentPath, currentRoot, currentRootLabel, currentDBEncFS);
                } else {
                    return super.onKeyDown(keyCode, event);
                }
            //}

            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
      if ((new File(path.get(info.position))).isDirectory()) {
          inflater.inflate(R.menu.file_dialog_context_menu_dir, menu);          
      } else {
          inflater.inflate(R.menu.file_dialog_context_menu, menu);
      }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      String pathName = path.get(info.position);
      switch (item.getItemId()) {
      case R.id.context_open:
        getIntent().putExtra(RESULT_EXPORT_PATHS, (String[])null);
        getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
        getIntent().putExtra(RESULT_OPEN_PATH, pathName);
        setResult(RESULT_OK, getIntent());
        finish();
        return true;
      case R.id.context_export:
          return true;
      default:
        return super.onContextItemSelected(item);
      }
    }

    @Override protected Dialog onCreateDialog(int id) {
        switch (id) {
         case NEW_FOLDER_DIALOG_ID:
             LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

             final View layout = inflater.inflate(R.layout.new_folder_dialog,
                     (ViewGroup) findViewById(R.id.new_folder_root));
             final EditText newFolder = (EditText) layout.findViewById(R.id.EditText_NewFolder);

             AlertDialog.Builder builder = new AlertDialog.Builder(this);
             builder.setTitle(R.string.title_new_folder);
             builder.setView(layout);
             builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int whichButton) {
                         removeDialog(NEW_FOLDER_DIALOG_ID);
                     }
                 });
             builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int which) {
                         String newFolderString = newFolder.getText().toString();
                         removeDialog(NEW_FOLDER_DIALOG_ID);
                         if (newFolderString.length() > 0) {
                             switch (selectionMode) {
                             case SelectionMode.MODE_OPEN_CREATE:
                             case SelectionMode.MODE_OPEN_CREATE_DB:
                                 new File(newFolderString).mkdir();
                                 break;
                             default:
                             }
                         } else {
                             showToast(R.string.new_folder_fail);
                         }
                     }
                 });
             return builder.create();
        }
        
        return null;
    }
    
    private void setSelectVisible(View v) {
        // layoutCreate.setVisibility(View.GONE);
        layoutSelect.setVisibility(View.VISIBLE);

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        selectButton.setEnabled(false);
    }
    
    private void showToast(int resId) {
        showToast(getString(resId));
    }
    
    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast err = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
                err.show();
            }
        });
    }
}