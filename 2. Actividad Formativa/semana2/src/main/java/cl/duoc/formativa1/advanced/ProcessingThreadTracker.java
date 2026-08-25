package cl.duoc.formativa1.advanced;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ProcessingThreadTracker {

    private final Set<String> threadNames = ConcurrentHashMap.newKeySet();

    public void recordCurrentThread() {
        threadNames.add(Thread.currentThread().getName());
    }

    public Set<String> snapshot() {
        return Collections.unmodifiableSet(Set.copyOf(threadNames));
    }

    public void reset() {
        threadNames.clear();
    }
}
