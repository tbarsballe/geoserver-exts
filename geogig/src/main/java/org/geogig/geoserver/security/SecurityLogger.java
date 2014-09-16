package org.geogig.geoserver.security;

import static java.lang.String.format;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import javax.annotation.Nullable;

import org.geogig.geoserver.config.LogStore;
import org.locationtech.geogig.api.AbstractGeoGigOp;
import org.locationtech.geogig.api.Context;
import org.locationtech.geogig.api.porcelain.CloneOp;
import org.locationtech.geogig.api.porcelain.FetchOp;
import org.locationtech.geogig.api.porcelain.PullOp;
import org.locationtech.geogig.api.porcelain.PushOp;
import org.locationtech.geogig.api.porcelain.RemoteAddOp;
import org.locationtech.geogig.api.porcelain.RemoteRemoveOp;
import org.locationtech.geogig.repository.Repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class SecurityLogger {

    private static final Map<Class<? extends AbstractGeoGigOp<?>>, MessageBuilder<?>> WATCHED_COMMANDS;
    static {
        Builder<Class<? extends AbstractGeoGigOp<?>>, MessageBuilder<?>> builder = ImmutableMap
                .builder();

        builder.put(RemoteAddOp.class, new RemoteAddMessageBuilder());
        builder.put(RemoteRemoveOp.class, new RemoteRemoveMessageBuilder());
        builder.put(PullOp.class, new PullOpMessageBuilder());
        builder.put(PushOp.class, new PushOpMessageBuilder());
        builder.put(FetchOp.class, new FetchOpMessageBuilder());
        builder.put(CloneOp.class, new CloneOpMessageBuilder());

        WATCHED_COMMANDS = builder.build();
    }

    private LogStore logStore;

    private static SecurityLogger INSTANCE;

    public SecurityLogger(LogStore logStore) {
        this.logStore = logStore;
        INSTANCE = this;
    }

    public static boolean interestedIn(Class<? extends AbstractGeoGigOp<?>> clazz) {
        return WATCHED_COMMANDS.containsKey(clazz);
    }

    public static void logPre(AbstractGeoGigOp<?> command) {
        if (INSTANCE == null) {
            return;// not yet initialized
        }
        INSTANCE.pre(command);
    }

    public static void logPost(AbstractGeoGigOp<?> command, @Nullable Object retVal,
            @Nullable RuntimeException exception) {

        if (INSTANCE == null) {
            return;// not yet initialized
        }
        if (exception == null) {
            INSTANCE.post(command, retVal);
        } else {
            INSTANCE.error(command, exception);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void error(AbstractGeoGigOp<?> command, RuntimeException exception) {
        MessageBuilder builder = builderFor(command);
        String repoUrl = repoUrl(command);
        logStore.error(repoUrl, builder.buildError(command, exception), exception);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void post(AbstractGeoGigOp<?> command, Object commandResult) {
        MessageBuilder builder = builderFor(command);
        String repoUrl = repoUrl(command);
        logStore.info(repoUrl, builder.buildPost(command, commandResult));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void pre(AbstractGeoGigOp<?> command) {
        MessageBuilder builder = builderFor(command);
        String repoUrl = repoUrl(command);
        logStore.debug(repoUrl, builder.buildPre(command));
    }

    private MessageBuilder<?> builderFor(AbstractGeoGigOp<?> command) {
        MessageBuilder<?> builder = WATCHED_COMMANDS.get(command.getClass());
        Preconditions.checkNotNull(builder);
        return builder;
    }

    @Nullable
    private String repoUrl(AbstractGeoGigOp<?> command) {
        Context context = command.context();
        if (context == null) {
            return null;
        }
        Repository repository = context.repository();
        if (repository == null) {
            return null;
        }
        URL location = repository.getLocation();
        if (location == null) {
            return null;
        }
        String uri;
        try {
            File f = new File(location.toURI());
            if (f.getName().endsWith(".geogig")) {
                f = f.getParentFile();
            }
            uri = f.getAbsolutePath();
        } catch (URISyntaxException e) {
            uri = location.toExternalForm();
        }
        return uri;
    }

    private static abstract class MessageBuilder<T extends AbstractGeoGigOp<?>> {

        CharSequence buildPost(T command, Object commandResult) {
            return format("%s success. Parameters: %s", friendlyName(), params(command));
        }

        CharSequence buildPre(T c) {
            return format("%s: Parameters: %s", friendlyName(), params(c));
        }

        CharSequence buildError(T command, RuntimeException exception) {
            return format("%s failed. Parameters: %s. Error message: %s", friendlyName(),
                    params(command), exception.getMessage());
        }

        abstract String friendlyName();

        abstract String params(T command);
    }

    private static class RemoteAddMessageBuilder extends MessageBuilder<RemoteAddOp> {
        @Override
        String friendlyName() {
            return "Remote add";
        }

        @Override
        String params(RemoteAddOp c) {
            return format("name='%s', url='%s'", c.getName(), c.getURL());
        }
    }

    private static class RemoteRemoveMessageBuilder extends MessageBuilder<RemoteRemoveOp> {
        @Override
        String friendlyName() {
            return "Remote remove";
        }

        @Override
        String params(RemoteRemoveOp c) {
            return format("name='%s'", c.getName());
        }
    }

    private static class PullOpMessageBuilder extends MessageBuilder<PullOp> {
        @Override
        String friendlyName() {
            return "Pull";
        }

        @Override
        String params(PullOp c) {
            return format("remote=%s, refSpecs=%s, depth=%s, author=%s, author email=%s",
                    c.getRemoteName(), c.getRefSpecs(), c.getDepth(), c.getAuthor(),
                    c.getAuthorEmail());
        }
    }

    private static class PushOpMessageBuilder extends MessageBuilder<PushOp> {
        @Override
        String friendlyName() {
            return "Push";
        }

        @Override
        String params(PushOp c) {
            return format("remote=%s, refSpecs=%s", c.getRemoteName(), c.getRefSpecs());
        }
    }

    private static class FetchOpMessageBuilder extends MessageBuilder<FetchOp> {
        @Override
        String friendlyName() {
            return "Fetch";
        }

        @Override
        String params(FetchOp c) {
            return format("remotes=%s, all=%s, full depth=%s, depth=%s, prune=%s",
                    c.getRemoteNames(), c.isAll(), c.isFullDepth(), c.getDepth(), c.isPrune());
        }
    }

    private static class CloneOpMessageBuilder extends MessageBuilder<CloneOp> {
        @Override
        String friendlyName() {
            return "Clone";
        }

        @Override
        String params(CloneOp c) {
            return format("url=%s, branch=%s, depth=%s", c.getRepositoryURL().orNull(), c
                    .getBranch().orNull(), c.getDepth().orNull());
        }
    }
}
