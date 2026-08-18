package com.evolvingfurnaces.tier;

import net.minecraft.client.Minecraft;

public final class EFClientDisplayNames {

    private EFClientDisplayNames() {
    }

    public static String selectedLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }

    static String resolve(EFDisplayNames names) {
        return names.resolve(selectedLanguage());
    }
}
