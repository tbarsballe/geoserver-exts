package org.geogig.geoserver.web.security;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.locationtech.geogig.api.AbstractGeoGigOp;
import org.locationtech.geogig.api.Remote;
import org.locationtech.geogig.api.hooks.CannotRunGeogigOperationException;
import org.locationtech.geogig.api.hooks.CommandHook;
import org.locationtech.geogig.api.plumbing.LsRemote;
import org.locationtech.geogig.api.porcelain.CloneOp;
import org.locationtech.geogig.api.porcelain.FetchOp;
import org.locationtech.geogig.api.porcelain.PushOp;
import org.geogig.geoserver.config.ConfigStore;
import org.geoserver.platform.GeoServerExtensions;

public final class NetworkSecurityHook implements CommandHook {
    public <C extends AbstractGeoGigOp<?>> C pre(C command) throws CannotRunGeogigOperationException {
        if (command instanceof LsRemote) {
            LsRemote lsRemote = (LsRemote) command;
            Optional<Remote> remote = lsRemote.getRemote();
            if (remote.isPresent()) {
                String url = remote.get().getFetchURL();
                if (isRestricted(url)) {
                    throw new CannotRunGeogigOperationException();
                }
            }
        } else if (command instanceof CloneOp) {
            CloneOp cloneOp = (CloneOp) command;
            Optional<String> url = cloneOp.getRepositoryURL();
            if (url.isPresent() && isRestricted(url.get())) {
                throw new CannotRunGeogigOperationException();
            }
        } else if (command instanceof FetchOp) {
            FetchOp fetchOp = (FetchOp) command;
            for (Remote r : fetchOp.getRemotes()) {
                if (isRestricted(r.getFetchURL())) {
                    throw new CannotRunGeogigOperationException();
                }
            }
        } else if (command instanceof PushOp) {
            PushOp pushOp = (PushOp) command;
            Optional<Remote> remote = pushOp.getRemote();
            if (remote.isPresent()) {
                String url = remote.get().getFetchURL();
                if (isRestricted(url)) {
                    throw new CannotRunGeogigOperationException();
                }
            }
        }

        return command;
    }

    @Override
    public <T> T post(AbstractGeoGigOp<T> command, Object retVal, RuntimeException potentialException) throws Exception {
        return (T) retVal;
    }

    public boolean appliesTo(Class<? extends AbstractGeoGigOp<?>> clazz) {
        return LsRemote.class.equals(clazz) ||
               CloneOp.class.equals(clazz) ||
               FetchOp.class.equals(clazz) ||
               PushOp.class.equals(clazz);
    }

    private final boolean isRestricted(String url) {
        ConfigStore configStore = (ConfigStore) GeoServerExtensions.bean("geogigConfigStore");
        try {
            List<WhitelistRule> rules = configStore.getWhitelist();
            for (WhitelistRule rule : rules) {
                if (ruleBlocks(rule, url)) {
                    return true;
                }
            }
        } catch (IOException e) {
            // LOG.error(e);
            return true; // fail closed
        }
        return false;
    }

    private final boolean ruleBlocks(WhitelistRule rule, String url) {
        URL parsed;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }

        if (parsed.getHost() == null || parsed.getProtocol() == null || parsed.getProtocol().equals("file")) {
            return false;
        }

        if (rule.isRequireSSL() && !parsed.getProtocol().equals("https")) {
            return true;
        }

        if (rule.getPattern().startsWith("[.*]")) {
            final String effectivePattern = rule.getPattern().substring("[.*]".length());
            return parsed.getHost().endsWith(effectivePattern);
        } else {
            return parsed.getHost().equals(rule.getPattern());
        }
    }
}
