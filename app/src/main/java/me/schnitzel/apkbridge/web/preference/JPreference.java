package me.schnitzel.apkbridge.web.preference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JPreference {
    public String key;
    public JCheckBoxPreference checkBoxPreference;
    public JSwitchPreference switchPreferenceCompat;
    public JListPreference listPreference;
    public JMultiSelectListPreference multiSelectListPreference;
    public JEditTextPreference editTextPreference;

    public JPreference() {}
}
