import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class GalaxyLogger {
    private static final List<Consumer<Event>> EVENT_HANDLERS = new ArrayList<>();

    private static VirtualMachine vm;

    private static void connectToJdwp(String port) {
        AttachingConnector connector = Bootstrap.virtualMachineManager()
                .attachingConnectors()
                .stream()
                .filter(c -> c.defaultArguments().containsKey("port"))
                .findAny()
                .orElseThrow(IllegalStateException::new);

        Map<String, Connector.Argument> arguments = connector.defaultArguments();
        arguments.get("port").setValue(port);

        try {
            vm = connector.attach(arguments);
        } catch (IOException | IllegalConnectorArgumentsException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T extends Event> void register(Class<T> type, Consumer<T> handler) {
        EVENT_HANDLERS.add(event -> {
            if (type.isInstance(event)) {
                handler.accept(type.cast(event));
            }
        });
    }

    private static void breakpointAt(ReferenceType clazz, String method, int line) {
        Method main = clazz.methodsByName(method)
                .stream()
                .findAny()
                .orElseThrow(IllegalArgumentException::new);

        Location location;
        try {
            location = main.allLineLocations().get(line);
        } catch (AbsentInformationException e) {
            throw new IllegalStateException(e);
        }

        BreakpointRequest breakpointRequest = vm.eventRequestManager().createBreakpointRequest(location);
        breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        breakpointRequest.enable();
    }

    private static void breakpointAt(String className, String method, int line) {
        Optional<ReferenceType> loadedClass = vm.classesByName(className).stream().findAny();

        loadedClass.ifPresent(classRef -> breakpointAt(classRef, method, line));

        if (!loadedClass.isPresent()) {
            ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
            classPrepareRequest.addClassFilter(className);
            classPrepareRequest.enable();

            register(ClassPrepareEvent.class, e -> breakpointAt(e.referenceType(), method, line));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        connectToJdwp(args[0]);
        breakpointAt("EnlightenedBrain", "main", 2);
        register(BreakpointEvent.class, bp -> {
            try {
                StackFrame frame = bp.thread().frame(0);
                LocalVariable delay = frame.visibleVariableByName("delay");
                Value value = frame.getValue(delay);
                System.out.printf("[%s] Sleeping for %s\n", Instant.now(), value);
            } catch (IncompatibleThreadStateException | AbsentInformationException e) {
                throw new IllegalStateException(e);
            }
        });

        EventQueue queue = vm.eventQueue();
        while (true) {
            vm.resume();

            queue.remove().forEach(event -> EVENT_HANDLERS.forEach(handler -> handler.accept(event)));
        }
    }
}
