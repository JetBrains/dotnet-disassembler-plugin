using System.Numerics;
using Some.Name.Space;

public class TestClass
{
    static void Main()
    {
        Console.WriteLine(Some.Name.Space.TestClass<int, TestClass>.Add(1, 5));
    }
    
    public static int A{caret}dd(int a, int b)
    {
        return LocalAdd(a, b);

        int LocalAdd(int a, int b)
        {
            return a + b;
        }
    }

    public TestClass(TestClass<int, TestClass> obj)
    {
        
    }
}

namespace Some.Name.Space
{
    public class TestClass<T, U> where T : INumber<T>
    {
        static void Main()
        {
            Console.WriteLine();
        }
    
        public static T Add(T a, T b)
        {
            return a + b;
        }
    } 
}


public struct TestStruct
{
    
}