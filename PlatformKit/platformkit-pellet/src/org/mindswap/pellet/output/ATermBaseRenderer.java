//The MIT License
//
//Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to
//deal in the Software without restriction, including without limitation the
//rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
//sell copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.

/*
 * Created on Oct 9, 2004
 */
package org.mindswap.pellet.output;

import java.io.Writer;

import aterm.ATermAppl;

/**
 * Base implementation of renderer interface to ease the implementation for different output
 * formats.
 * 
 * @author Evren Sirin
 */
public abstract class ATermBaseRenderer extends ATermBaseVisitor implements ATermRenderer {
    OutputFormatter out;

    /* (non-Javadoc)
     * @see org.mindswap.pellet.utils.ATermRenderer#setWriter(org.mindswap.pellet.utils.OutputFormatter)
     */
    public void setWriter(OutputFormatter out) {
        this.out = out;
    }

    /* (non-Javadoc)
     * @see org.mindswap.pellet.utils.ATermRenderer#getWriter()
     */
    public OutputFormatter getWriter() {
        return out;
    }

    /* (non-Javadoc)
     * @see org.mindswap.pellet.utils.ATermRenderer#setWriter(java.io.Writer)
     */
    public void setWriter(Writer out) {
        this.out = new OutputFormatter(out, false);
    }

    /* (non-Javadoc)
     * @see org.mindswap.pellet.utils.ATermVisitor#visitTerm(aterm.ATermAppl)
     */
    public void visitTerm(ATermAppl term) {
        out.printURI(term.getName());
    }
}
