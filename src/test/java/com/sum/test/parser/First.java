package com.sum.test.parser;

import java.io.BufferedReader;
import java.io.FileReader;

import org.junit.Assert;
import org.junit.Test;

import com.sum.frontend.FrontendFactory;
import com.sum.frontend.Source;
import com.sum.frontend.java.JavaParserTD;
import com.sum.frontend.java.parser.PackageParser;

public class First {
    
    @Test
    public void test() throws Exception{
        Source source = new Source(new BufferedReader(new FileReader("/projects/javaparser/src/test/resources/Data.txt")));
        Assert.assertNotNull(source);
        JavaParserTD parser = (JavaParserTD)FrontendFactory.createParser("Java", "top-down", source);
        PackageParser packageParser = new PackageParser(parser);
        packageParser.parse();
    }
}
