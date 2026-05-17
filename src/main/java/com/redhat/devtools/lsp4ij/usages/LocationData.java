package com.redhat.devtools.lsp4ij.usages;

import com.redhat.devtools.lsp4ij.LanguageServerItem;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.Nullable;

public record LocationData(Location location, LanguageServerItem languageServer, @Nullable Range originRange) {

    public LocationData(Location location, LanguageServerItem languageServer) {
        this(location, languageServer, null);
    }
}
