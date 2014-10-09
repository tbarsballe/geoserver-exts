package org.geogig.geoserver.security;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.geogig.geoserver.config.ConfigStore;
import org.geogig.geoserver.config.WhitelistRule;
import org.geoserver.platform.GeoServerExtensions;
import org.locationtech.geogig.api.AbstractGeoGigOp;
import org.locationtech.geogig.api.Remote;
import org.locationtech.geogig.api.hooks.CannotRunGeogigOperationException;
import org.locationtech.geogig.api.hooks.CommandHook;
import org.locationtech.geogig.api.plumbing.LsRemote;
import org.locationtech.geogig.api.porcelain.CloneOp;
import org.locationtech.geogig.api.porcelain.FetchOp;
import org.locationtech.geogig.api.porcelain.PushOp;

import com.google.common.base.Optional;

/**
 * Classpath {@link CommandHook hook} that catches remotes related commands before they are executed
 * and validates them against the {@link WhitelistRule whitelist rules} to let them process or not.
 *
 */
public final class NetworkSecurityHook implements CommandHook {

    @Override
    public <C extends AbstractGeoGigOp<?>> C pre(C command)
            throws CannotRunGeogigOperationException {
        if (command instanceof LsRemote) {
            LsRemote lsRemote = (LsRemote) command;
            Optional<Remote> remote = lsRemote.getRemote();
            if (remote.isPresent()) {
                String url = remote.get().getFetchURL();
                checkRestricted(url);
            }
        } else if (command instanceof CloneOp) {
            CloneOp cloneOp = (CloneOp) command;
            Optional<String> url = cloneOp.getRepositoryURL();
            if (url.isPresent()) {
                checkRestricted(url.get());
            }
        } else if (command instanceof FetchOp) {
            FetchOp fetchOp = (FetchOp) command;
            for (Remote r : fetchOp.getRemotes()) {
                checkRestricted(r.getFetchURL());
            }
        } else if (command instanceof PushOp) {
            PushOp pushOp = (PushOp) command;
            Optional<Remote> remote = pushOp.getRemote();
            if (remote.isPresent()) {
                String url = remote.get().getPushURL();
                checkRestricted(url);
            }
        }

        return command;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T post(AbstractGeoGigOp<T> command, Object retVal,
            RuntimeException potentialException) throws Exception {
        return (T) retVal;
    }

    @Override
    public boolean appliesTo(Class<? extends AbstractGeoGigOp<?>> clazz) {
        return LsRemote.class.equals(clazz) || CloneOp.class.equals(clazz)
                || FetchOp.class.equals(clazz) || PushOp.class.equals(clazz);
    }

    private final void checkRestricted(String remoteUrl) throws CannotRunGeogigOperationException {

        ConfigStore configStore = (ConfigStore) GeoServerExtensions.bean("geogigConfigStore");
        List<WhitelistRule> rules;
        try {
            rules = configStore.getWhitelist();
        } catch (IOException e) {
            throw new CannotRunGeogigOperationException("Unable to obtain the remotes white list: "
                    + e.getMessage(), e);
        }
        for (WhitelistRule rule : rules) {
            if (ruleBlocks(rule, remoteUrl)) {
                String msg = String.format("Blocked %s. Remote: %s", rule, remoteUrl);
                throw new CannotRunGeogigOperationException(msg);
            }
        }
    }

    private final boolean ruleBlocks(WhitelistRule rule, String url) {
        URL parsed;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }

        final String host = parsed.getHost();
        if (host == null || parsed.getProtocol() == null || parsed.getProtocol().equals("file")) {
            return false;
        }

        if (rule.isRequireSSL() && !parsed.getProtocol().equals("https")) {
            return true;
        }

        if (rule.getPattern().startsWith("[.*]")) {
            final String effectivePattern = rule.getPattern().substring("[.*]".length());
            return host.endsWith(effectivePattern);
        } else {
            return host.equals(rule.getPattern());
        }
    }
}
