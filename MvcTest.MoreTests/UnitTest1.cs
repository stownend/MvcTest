using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using MvcTest;

namespace MvcTest.MoreTests
{
    [TestClass]
    public class UnitTest1
    {
        [TestMethod]
        public void MoreTests_TestMethod1()
        {
            MvcTest.Models.Person = new Person();
        }
    }
}
