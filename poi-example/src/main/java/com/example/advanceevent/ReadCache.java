package com.example.advanceevent;

import com.example.utils.FileUtils;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.Ehcache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author: rongdi
 * @date:
 */
public class ReadCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(Ehcache.class);
    private int index = 0;
    private HashMap<Integer, String> dataMap = new HashMap(1334);
    private static CacheManager fileCacheManager;
    private static CacheConfiguration<Integer, HashMap> fileCacheConfiguration;
    private static CacheManager activeCacheManager;
    private CacheConfiguration<Integer, HashMap> activeCacheConfiguration;
    private Cache<Integer, HashMap> fileCache;
    private Cache<Integer, HashMap> activeCache;
    private String cacheAlias;
    private int cacheMiss = 0;

    public ReadCache(int maxCacheActivateSize) {
        this.activeCacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, HashMap.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap((long)maxCacheActivateSize, MemoryUnit.MB)).withSizeOfMaxObjectGraph(1000000L).withSizeOfMaxObjectSize((long)maxCacheActivateSize, MemoryUnit.MB).build();
        init();
    }

    private void init() {
        this.cacheAlias = UUID.randomUUID().toString();
        this.fileCache = fileCacheManager.createCache(this.cacheAlias, fileCacheConfiguration);
        this.activeCache = activeCacheManager.createCache(this.cacheAlias, this.activeCacheConfiguration);
    }

    public void put(String value) {
        this.dataMap.put(this.index, value);
        if ((this.index + 1) % 1000 == 0) {
            this.fileCache.put(this.index / 1000, this.dataMap);
            this.dataMap = new HashMap(1334);
        }

        ++this.index;
        if (LOGGER.isDebugEnabled() && this.index % 1000000 == 0) {
            LOGGER.debug("Already put :{}", this.index);
        }

    }

    public String get(Integer key) {
        if (key != null && key >= 0) {
            int route = key / 1000;
            HashMap<Integer, String> dataMap = (HashMap)this.activeCache.get(route);
            if (dataMap == null) {
                dataMap = (HashMap)this.fileCache.get(route);
                this.activeCache.put(route, dataMap);
                if (LOGGER.isDebugEnabled() && this.cacheMiss++ % 1000 == 0) {
                    LOGGER.debug("Cache misses count:{}", this.cacheMiss);
                }
            }

            return (String)dataMap.get(key);
        } else {
            return null;
        }
    }

    public void putFinished() {
        if (this.dataMap != null) {
            this.fileCache.put(this.index / 1000, this.dataMap);
        }
    }

    public void destroy() {
        fileCacheManager.removeCache(this.cacheAlias);
        activeCacheManager.removeCache(this.cacheAlias);
    }

    static {
        File cacheFile = FileUtils.createCacheTmpFile();
        fileCacheManager = CacheManagerBuilder.newCacheManagerBuilder().with(CacheManagerBuilder.persistence(cacheFile)).build(true);
        activeCacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
        fileCacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, HashMap.class, ResourcePoolsBuilder.newResourcePoolsBuilder().disk(10L, MemoryUnit.GB)).withSizeOfMaxObjectGraph(1000000L).withSizeOfMaxObjectSize(10L, MemoryUnit.GB).build();
    }

}
