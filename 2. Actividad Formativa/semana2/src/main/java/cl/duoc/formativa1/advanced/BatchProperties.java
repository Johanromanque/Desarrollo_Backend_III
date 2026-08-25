package cl.duoc.formativa1.advanced;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.batch")
public class BatchProperties implements InitializingBean {

    private int chunkSize = 5;
    private int corePoolSize = 3;
    private int maxPoolSize = 3;
    private int queueCapacity = 15;

    @Override
    public void afterPropertiesSet() {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("app.batch.chunk-size debe ser mayor que cero");
        }
        if (corePoolSize <= 0 || maxPoolSize <= 0) {
            throw new IllegalArgumentException("Los tamanos del pool deben ser mayores que cero");
        }
        if (maxPoolSize < corePoolSize) {
            throw new IllegalArgumentException("app.batch.max-pool-size debe ser mayor o igual a core-pool-size");
        }
        if (queueCapacity < 0) {
            throw new IllegalArgumentException("app.batch.queue-capacity no puede ser negativa");
        }
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
}
