package com.mawl.easycode.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ExceptionUtil;
import com.mawl.easycode.constants.StrState;
import com.mawl.easycode.dict.GlobalDict;
import com.mawl.easycode.dto.GenerateOptions;
import com.mawl.easycode.dto.SettingsStorageDTO;
import com.mawl.easycode.entity.TableInfo;
import com.mawl.easycode.entity.Template;
import com.mawl.easycode.service.CodeGenerateService;
import com.mawl.easycode.service.SettingsStorageService;
import com.mawl.easycode.service.TableInfoSettingsService;
import com.mawl.easycode.tool.CacheDataUtils;
import com.mawl.easycode.tool.ModuleUtils;
import com.mawl.easycode.tool.ProjectUtils;
import com.mawl.easycode.tool.StringUtils;
import com.mawl.easycode.ui.component.TemplateSelectComponent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 选择保存路径
 *
 * @author makejava
 * @version 1.0.0
 * @since 2018/07/17 13:10
 */
public class SelectSavePath extends DialogWrapper {
    /**
     * 主面板
     */
    private JPanel contentPane;
    /**
     * 模型下拉框
     */
    private JComboBox<String> moduleComboBox;
    /**
     * 包字段
     */
    private JTextField packageField;
    /**
     * 路径字段
     */
    private JTextField pathField;
    /**
     * 前缀字段
     */
    private JTextField preField;
    /**
     * 包选择按钮
     */
    private JButton packageChooseButton;
    /**
     * 路径选择按钮
     */
    private JButton pathChooseButton;
    /**
     * 模板面板
     */
    private JPanel templatePanel;
    /**
     * 统一配置复选框
     */
    private JCheckBox unifiedConfigCheckBox;
    /**
     * 弹框选是复选框
     */
    private JCheckBox titleSureCheckBox;
    /**
     * 格式化代码复选框
     */
    private JCheckBox reFormatCheckBox;
    /**
     * 弹框全否复选框
     */
    private JCheckBox titleRefuseCheckBox;
    /**
     * 数据缓存工具类
     */
    private CacheDataUtils cacheDataUtils = CacheDataUtils.getInstance();
    /**
     * 表信息服务
     */
    private TableInfoSettingsService tableInfoService;
    /**
     * 项目对象
     */
    private Project project;
    /**
     * 代码生成服务
     */
    private CodeGenerateService codeGenerateService;
    /**
     * 当前项目中的module
     */
    private List<Module> moduleList;

    /**
     * 实体模式生成代码
     */
    private boolean entityMode;

    /**
     * 模板选择组件
     */
    private TemplateSelectComponent templateSelectComponent;

    private static final String LAST_USED_PREFIX = "EasyCode.LastUsed.";
    private static final String KEY_MODULE_NAME = LAST_USED_PREFIX + "ModuleName";
    private static final String KEY_PACKAGE_NAME = LAST_USED_PREFIX + "PackageName";
    private static final String KEY_PRE_NAME = LAST_USED_PREFIX + "PreName";
    private static final String KEY_TEMPLATE_GROUP = LAST_USED_PREFIX + "TemplateGroupName";
    private static final String KEY_SELECTED_TEMPLATES = LAST_USED_PREFIX + "SelectedTemplates";
    private static final String KEY_RE_FORMAT = LAST_USED_PREFIX + "ReFormat";
    private static final String KEY_TITLE_SURE = LAST_USED_PREFIX + "TitleSure";
    private static final String KEY_TITLE_REFUSE = LAST_USED_PREFIX + "TitleRefuse";
    private static final String KEY_UNIFIED_CONFIG = LAST_USED_PREFIX + "UnifiedConfig";

    /**
     * 构造方法
     */
    public SelectSavePath(Project project) {
        this(project, false);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.contentPane;
    }

    /**
     * 构造方法
     */
    public SelectSavePath(Project project, boolean entityMode) {
        super(project);
        this.entityMode = entityMode;
        this.project = project;
        this.tableInfoService = TableInfoSettingsService.getInstance();
        this.codeGenerateService = CodeGenerateService.getInstance(project);
        // 初始化module，存在资源路径的排前面
        this.moduleList = new LinkedList<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            // 存在源代码文件夹放前面，否则放后面
            if (ModuleUtils.existsSourcePath(module)) {
                this.moduleList.add(0, module);
            } else {
                this.moduleList.add(module);
            }
        }
        this.initPanel();
        this.refreshData();
        this.initEvent();
        init();
        setTitle(GlobalDict.TITLE_INFO);
        //初始化路径
        refreshPath();
    }

    private void initEvent() {
        //监听module选择事件
        moduleComboBox.addActionListener(e -> {
            // 刷新路径
            refreshPath();
        });

        try {
            Class<?> cls = Class.forName("com.intellij.ide.util.PackageChooserDialog");
            //添加包选择事件
            packageChooseButton.addActionListener(e -> {
                try {
                    Constructor<?> constructor = cls.getConstructor(String.class, Project.class);
                    Object dialog = constructor.newInstance("Package Chooser", project);
                    // 显示窗口
                    Method showMethod = cls.getMethod("show");
                    showMethod.invoke(dialog);
                    // 获取选中的包名
                    Method getSelectedPackageMethod = cls.getMethod("getSelectedPackage");
                    Object psiPackage = getSelectedPackageMethod.invoke(dialog);
                    if (psiPackage != null) {
                        Method getQualifiedNameMethod = psiPackage.getClass().getMethod("getQualifiedName");
                        String packageName = (String) getQualifiedNameMethod.invoke(psiPackage);
                        packageField.setText(packageName);
                        // 刷新路径
                        refreshPath();
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e1) {
                    ExceptionUtil.rethrow(e1);
                }
            });

            // 添加包编辑框失去焦点事件
            packageField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    // 刷新路径
                    refreshPath();
                }
            });
        } catch (ClassNotFoundException e) {
            // 没有PackageChooserDialog，并非支持Java的IDE，禁用相关UI组件
            packageField.setEnabled(false);
            packageChooseButton.setEnabled(false);
        }

        //选择路径
        pathChooseButton.addActionListener(e -> {
            //将当前选中的model设置为基础路径
            VirtualFile path = ProjectUtils.getBaseDir(project);
            Module module = getSelectModule();
            if (module != null) {
                path = ModuleUtils.getSourcePath(module);
            }
            VirtualFile virtualFile = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, path);
            if (virtualFile != null) {
                pathField.setText(virtualFile.getPath());
            }
        });
    }

    private void refreshData() {
        // 获取选中的表信息（鼠标右键的那张表），并提示未知类型
        TableInfo tableInfo;
        if(entityMode) {
            tableInfo = tableInfoService.getTableInfo(cacheDataUtils.getSelectPsiClass());
        } else {
            tableInfo = tableInfoService.getTableInfo(cacheDataUtils.getSelectDbTable());
        }

        boolean hasTableModuleSetting = false;
        boolean hasTableTemplateGroup = false;

        // 设置默认配置信息
        if (!StringUtils.isEmpty(tableInfo.getSaveModelName())) {
            moduleComboBox.setSelectedItem(tableInfo.getSaveModelName());
            hasTableModuleSetting = true;
        }
        if (!StringUtils.isEmpty(tableInfo.getSavePackageName())) {
            packageField.setText(tableInfo.getSavePackageName());
        }
        if (!StringUtils.isEmpty(tableInfo.getPreName())) {
            preField.setText(tableInfo.getPreName());
        }
        SettingsStorageDTO settings = SettingsStorageService.getSettingsStorage();
        String groupName = settings.getCurrTemplateGroupName();
        if (!StringUtils.isEmpty(tableInfo.getTemplateGroupName())) {
            if (settings.getTemplateGroupMap().containsKey(tableInfo.getTemplateGroupName())) {
                groupName = tableInfo.getTemplateGroupName();
                hasTableTemplateGroup = true;
            }
        }
        templateSelectComponent.setSelectedGroupName(groupName);
        String savePath = tableInfo.getSavePath();
        if (!StringUtils.isEmpty(savePath)) {
            // 判断是否需要拼接项目路径
            if (savePath.startsWith(StrState.RELATIVE_PATH)) {
                String projectPath = project.getBasePath();
                savePath = projectPath + savePath.substring(1);
            }
            pathField.setText(savePath);
        }

        // 对于没有保存过配置的表，使用上次生成的设置作为默认值
        applyLastUsedSettings(hasTableModuleSetting, hasTableTemplateGroup);
    }

    @Override
    protected void doOKAction() {
        onOK();
        super.doOKAction();
    }

    /**
     * 确认按钮回调事件
     */
    private void onOK() {
        List<Template> selectTemplateList = templateSelectComponent.getAllSelectedTemplate();
        // 如果选择的模板是空的
        if (selectTemplateList.isEmpty()) {
            Messages.showWarningDialog("Can't Select Template!", GlobalDict.TITLE_INFO);
            return;
        }
        String savePath = pathField.getText();
        if (StringUtils.isEmpty(savePath)) {
            Messages.showWarningDialog("Can't Select Save Path!", GlobalDict.TITLE_INFO);
            return;
        }
        // 针对Linux系统路径做处理
        savePath = savePath.replace("\\", "/");
        // 保存路径使用相对路径
        String basePath = project.getBasePath();
        if (!StringUtils.isEmpty(basePath) && savePath.startsWith(basePath)) {
            if (savePath.length() > basePath.length()) {
                if ("/".equals(savePath.substring(basePath.length(), basePath.length() + 1))) {
                    savePath = savePath.replace(basePath, ".");
                }
            } else {
                savePath = savePath.replace(basePath, ".");
            }
        }
        // 保存配置
        TableInfo tableInfo;
        if(!entityMode) {
            tableInfo = tableInfoService.getTableInfo(cacheDataUtils.getSelectDbTable());
        } else {
            tableInfo = tableInfoService.getTableInfo(cacheDataUtils.getSelectPsiClass());
        }
        tableInfo.setSavePath(savePath);
        tableInfo.setSavePackageName(packageField.getText());
        tableInfo.setPreName(preField.getText());
        tableInfo.setTemplateGroupName(templateSelectComponent.getselectedGroupName());
        Module module = getSelectModule();
        if (module != null) {
            tableInfo.setSaveModelName(module.getName());
        }
        // 保存配置
        tableInfoService.saveTableInfo(tableInfo);

        // 保存本次选择作为下次新表的默认值
        saveLastUsedSettings();

        // 生成代码
        codeGenerateService.generate(selectTemplateList, getGenerateOptions());
    }

    /**
     * 初始化方法
     */
    private void initPanel() {
        // 初始化模板组
        this.templateSelectComponent = new TemplateSelectComponent();
        templatePanel.add(this.templateSelectComponent.getMainPanel(), BorderLayout.CENTER);

        //初始化Module选择
        for (Module module : this.moduleList) {
            moduleComboBox.addItem(module.getName());
        }
    }

    /**
     * 获取生成选项
     *
     * @return {@link GenerateOptions}
     */
    private GenerateOptions getGenerateOptions() {
        return GenerateOptions.builder()
                .entityModel(this.entityMode)
                .reFormat(reFormatCheckBox.isSelected())
                .titleSure(titleSureCheckBox.isSelected())
                .titleRefuse(titleRefuseCheckBox.isSelected())
                .unifiedConfig(unifiedConfigCheckBox.isSelected())
                .build();
    }

    /**
     * 获取选中的Module
     *
     * @return 选中的Module
     */
    private Module getSelectModule() {
        String name = (String) moduleComboBox.getSelectedItem();
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        return ModuleManager.getInstance(project).findModuleByName(name);
    }

    /**
     * 获取基本路径
     *
     * @return 基本路径
     */
    private String getBasePath() {
        Module module = getSelectModule();
        // 未选中 module 时，回退使用第一个可用 module（moduleList 已把有源码根的排在最前）
        if (module == null && !moduleList.isEmpty()) {
            module = moduleList.get(0);
        }
        if (module != null) {
            VirtualFile virtualFile = ModuleUtils.getSourcePath(module);
            if (virtualFile != null) {
                // 返回源代码根目录（如 src/main/java）
                return virtualFile.getPath();
            }
        }
        // 仍拿不到 module/源码根时，才退回项目根目录（保持原有兜底行为）
        VirtualFile baseVirtualFile = ProjectUtils.getBaseDir(project);
        if (baseVirtualFile == null) {
            Messages.showWarningDialog("无法获取到项目基本路径！", GlobalDict.TITLE_INFO);
            return "";
        }
        return baseVirtualFile.getPath();
    }

    /**
     * 刷新目录
     */
    private void refreshPath() {
        String packageName = packageField.getText();
        // 获取基本路径
        String path = getBasePath();
        // 兼容Linux路径
        path = path.replace("\\", "/");
        // 如果存在包路径，添加包路径
        if (!StringUtils.isEmpty(packageName)) {
            path += "/" + packageName.replace(".", "/");
        }
        pathField.setText(path);
    }

    private void saveLastUsedSettings() {
        PropertiesComponent props = PropertiesComponent.getInstance(project);
        Module module = getSelectModule();
        if (module != null) {
            props.setValue(KEY_MODULE_NAME, module.getName());
        }
        props.setValue(KEY_PACKAGE_NAME, packageField.getText());
        props.setValue(KEY_PRE_NAME, preField.getText());
        props.setValue(KEY_TEMPLATE_GROUP, templateSelectComponent.getselectedGroupName());
        List<String> selectedTemplates = templateSelectComponent.getSelectedTemplateNames();
        if (!selectedTemplates.isEmpty()) {
            props.setValue(KEY_SELECTED_TEMPLATES, String.join(",", selectedTemplates));
        }
        props.setValue(KEY_RE_FORMAT, String.valueOf(reFormatCheckBox.isSelected()));
        props.setValue(KEY_TITLE_SURE, String.valueOf(titleSureCheckBox.isSelected()));
        props.setValue(KEY_TITLE_REFUSE, String.valueOf(titleRefuseCheckBox.isSelected()));
        props.setValue(KEY_UNIFIED_CONFIG, String.valueOf(unifiedConfigCheckBox.isSelected()));
    }

    private void applyLastUsedSettings(boolean hasTableModuleSetting, boolean hasTableTemplateGroup) {
        PropertiesComponent props = PropertiesComponent.getInstance(project);

        if (!hasTableModuleSetting) {
            String lastModule = props.getValue(KEY_MODULE_NAME);
            if (!StringUtils.isEmpty(lastModule)) {
                moduleComboBox.setSelectedItem(lastModule);
            }
        }
        if (StringUtils.isEmpty(packageField.getText())) {
            String lastPackage = props.getValue(KEY_PACKAGE_NAME);
            if (!StringUtils.isEmpty(lastPackage)) {
                packageField.setText(lastPackage);
            }
        }
        if (StringUtils.isEmpty(preField.getText())) {
            String lastPreName = props.getValue(KEY_PRE_NAME);
            if (!StringUtils.isEmpty(lastPreName)) {
                preField.setText(lastPreName);
            }
        }
        if (!hasTableTemplateGroup) {
            String lastTemplateGroup = props.getValue(KEY_TEMPLATE_GROUP);
            if (!StringUtils.isEmpty(lastTemplateGroup)) {
                templateSelectComponent.setSelectedGroupName(lastTemplateGroup);
            }
        }
        // 恢复上次选中的模板（Controller/Service/Entity 等）
        String lastSelectedTemplates = props.getValue(KEY_SELECTED_TEMPLATES);
        if (!StringUtils.isEmpty(lastSelectedTemplates)) {
            templateSelectComponent.setSelectedTemplates(Arrays.asList(lastSelectedTemplates.split(",")));
        }
        // 复选框没有按表持久化，始终使用上次的值
        String lastReFormat = props.getValue(KEY_RE_FORMAT);
        if (lastReFormat != null) {
            reFormatCheckBox.setSelected(Boolean.parseBoolean(lastReFormat));
        }
        String lastTitleSure = props.getValue(KEY_TITLE_SURE);
        if (lastTitleSure != null) {
            titleSureCheckBox.setSelected(Boolean.parseBoolean(lastTitleSure));
        }
        String lastTitleRefuse = props.getValue(KEY_TITLE_REFUSE);
        if (lastTitleRefuse != null) {
            titleRefuseCheckBox.setSelected(Boolean.parseBoolean(lastTitleRefuse));
        }
        String lastUnifiedConfig = props.getValue(KEY_UNIFIED_CONFIG);
        if (lastUnifiedConfig != null) {
            unifiedConfigCheckBox.setSelected(Boolean.parseBoolean(lastUnifiedConfig));
        }
    }
}
