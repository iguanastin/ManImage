package manimage.common.settings;

public interface SettingListener<T> {

    void settingChanged(T oldValue, T newValue);

}
