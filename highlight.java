Predicate<Integer> pserwith = new PredicateSerializeWith();

@SerializeFunctionWith(PredicateSerializeWithExt.class)
private static final class PredicateSerializeWith implements Predicate<Integer> {
   public boolean test(Integer i) { return i > 70; }
}

public static final class PredicateSerializeWithExt implements Externalizer<Object> {
   public void writeObject(ObjectOutput oo, Object o) {}
   public Object readObject(ObjectInput input) { return new PredicateSerializeWith(); }
}
