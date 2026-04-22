package com.mawl.easycode.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.mawl.easycode.dto.SettingsStorageDTO;
import com.mawl.easycode.service.impl.SettingsStorageServiceImpl;

/**
 * @author makejava
 * @version 1.0.0
 * @date 2021/08/07 11:55
 */
public interface SettingsStorageService extends PersistentStateComponent<SettingsStorageDTO> {
    /**
     * 获取实例
     *
     * @return {@link SettingsStorageService}
     */
    static SettingsStorageService getInstance() {
        return ApplicationManager.getApplication().getService(SettingsStorageServiceImpl.class);
    }

    /**
     * 获取设置存储
     *
     * @return {@link SettingsStorageDTO}
     */
    static SettingsStorageDTO getSettingsStorage() {
        return getInstance().getState();
    }
}
