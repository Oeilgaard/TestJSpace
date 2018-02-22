/*******************************************************************************
 * Copyright (c) 2017 Michele Loreti and the jSpace Developers (see the included 
 * authors file).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/
package ProducerConsumer.src.main.java.org.jspace.examples.pc;

import org.jspace.SequentialSpace;
import org.jspace.Space;

/**
 * @author loreti
 *
 */
public class ProducerConsumer {
	
	
	public static void main( String[] argv ) throws InterruptedException {
		Space space = new SequentialSpace();

		Thread t1 = new Thread( new ProducerAgent("hammer", space,10) );
		Thread t2 = new Thread( new ProducerAgent("anvil", space,10) );
		Thread t3 = new Thread( new ConsumerAgent(space,20) );

		t3.start();
		t1.start();
		t2.start();
		
		
		t1.join();
		t2.join();
		t3.join();
		
	}
	

}
