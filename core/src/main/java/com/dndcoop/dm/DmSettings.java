package com.dndcoop.dm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DmSettings {

    public enum Provider {
        OLLAMA("Ollama (Local Default)", "http://localhost:11434", "llama3.2"),
        LM_STUDIO("LM Studio (Local)", "http://localhost:1234", "local-model"),
        OPENAI_COMPATIBLE("Custom OpenAI-Compatible", "http://localhost:8080", "local-model"),
        OFFLINE_ONLY("Offline Rule-Based DM Only", "", "");

        private final String displayName;
        private final String defaultUrl;
        private final String defaultModel;

        Provider(String displayName, String defaultUrl, String defaultModel) {
            this.displayName = displayName;
            this.defaultUrl = defaultUrl;
            this.defaultModel = defaultModel;
        }

        public String getDisplayName() { return displayName; }
        public String getDefaultUrl() { return defaultUrl; }
        public String getDefaultModel() { return defaultModel; }
    }

    public enum FontScale {
        NORMAL("Normal", 1.0f),
        LARGE("Large", 1.22f),
        TV("TV / Couch", 1.42f);

        private final String label;
        private final float multiplier;

        FontScale(String label, float multiplier) {
            this.label = label;
            this.multiplier = multiplier;
        }

        public String getLabel() { return label; }
        public float getMultiplier() { return multiplier; }

        public FontScale next() {
            FontScale[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    private Provider provider = Provider.OLLAMA;
    private String baseUrl = Provider.OLLAMA.getDefaultUrl();
    private String modelName = Provider.OLLAMA.getDefaultModel();
    private boolean autoRollDice = false;
    private FontScale fontScale = FontScale.NORMAL;
    private int timeoutSeconds = 18;

    public DmSettings() {
    }

    public void applyPreset(Provider preset) {
        this.provider = preset;
        if (preset != Provider.OFFLINE_ONLY) {
            this.baseUrl = preset.getDefaultUrl();
            this.modelName = preset.getDefaultModel();
        }
    }

    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider != null ? provider : Provider.OLLAMA; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl != null ? baseUrl : ""; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName != null ? modelName : ""; }
    public boolean isAutoRollDice() { return autoRollDice; }
    public void setAutoRollDice(boolean autoRollDice) { this.autoRollDice = autoRollDice; }
    public FontScale getFontScale() { return fontScale; }
    public void setFontScale(FontScale fontScale) { this.fontScale = fontScale != null ? fontScale : FontScale.NORMAL; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = Math.max(3, timeoutSeconds); }
}
