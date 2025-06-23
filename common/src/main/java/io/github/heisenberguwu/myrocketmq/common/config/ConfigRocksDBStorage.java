package io.github.heisenberguwu.myrocketmq.common.config;

public class ConfigRocksDBStorage extends AbstractRocksDBStorage {

    @Override
    protected boolean postLoad() {
        return false;
    }

    @Override
    protected void preShutdown() {

    }
}