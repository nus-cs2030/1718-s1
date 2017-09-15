class A {
  protected int x = 0;
}
class B extends A {
  public int x = 1;
  void f() {
    System.out.println(super.x);
  }
}
class Main {
  public static void main(String[] args) {
    B b = new B();
    b.f();
  }
}
