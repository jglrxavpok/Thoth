package thoth.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;

public abstract class TranslationSet {

    private final MethodHandles.Lookup lookup;
    private final HashMap<String, MethodHandle> handles;

    public TranslationSet() {
        lookup = MethodHandles.lookup();
        handles = new HashMap<>();
        initHandles();
    }

    protected abstract void initHandles();

    protected void registerHandle(String name, int argsNbr) {
        Class<?>[] args = new Class<?>[argsNbr];
        Arrays.fill(args, ThothValue.class);
        MethodType type = MethodType.methodType(Translation.class, args);
        try {
            MethodHandle handle = lookup.findVirtual(getClass(), name, type);
            handles.put(name, handle);
            System.out.println("Successfully created handle for "+name+" "+handle+ " in class "+getClass().getCanonicalName());
        } catch (NoSuchMethodException e) {
            System.err.println("Invalid translation id: " + name + " with " + argsNbr + " arguments. Check the name and the arguments count! "+e.getMessage());
        } catch (IllegalAccessException e) {
            System.err.println("Invalid translation id: " + name+". Check the name and check that the translation actually exists!");
        }
    }

    public Translation getTranslation(String id) {
        return getTranslation(id, new ThothValue[0]);
    }

    public Translation getTranslation(String id, Translation... args) {
        ThothValue[] newArgs = new ThothValue[args.length];
        for(int i = 0;i<args.length;i++) {
            newArgs[i] = new TranslationValue(args[i]);
        }
        return getTranslationWithArray(id, newArgs);
    }

    public Translation getTranslation(String id, ThothValue... args) {
        return getTranslationWithArray(id, args);
    }

    public Translation getTranslationWithArray(String id, ThothValue[] args) {
        MethodHandle handle = handles.get(id);
        if(handle == null) {
            return new Translation(0, id, new String[0]);
        }
        try {
            TranslationSet value = this;
            if(handle.type().parameterCount() == 1)
                return (Translation)handle.invoke(value);
            else {
                List<Object> list = new ArrayList<>();
                list.add(value);
                for(ThothValue t : args) {
                    list.add(t);
                }
                while(list.size() < handle.type().parameterCount()) {
                    list.add(new NullValue());
                }

                while(list.size() > handle.type().parameterCount()) {
                    list.remove(list.size()-1);
                }
                return (Translation) handle.invokeWithArguments(list);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }
}
