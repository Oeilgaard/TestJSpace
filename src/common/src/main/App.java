package common.src.main;

import org.jspace.*;

import java.io.IOException;
import java.util.List;

public class App {

	public static void main(String[] argv) throws InterruptedException, IOException {

		/*
			TEST 1: Test hvordan spaces og tuples bruges. Get og query returnerer Object[]
			Put bruger typerne den modtager og lægger dem i en tuple.
		 */
//		Space inbox = new SequentialSpace();
//
//		Tuple testTuple1 = new Tuple("Test","2");
//		System.out.println(testTuple1.getElementAt(0) + " " + testTuple1.getElementAt(1));
//
//		inbox.put("Hello World!","Hej");
//		inbox.put("Number 2");
//		inbox.put(testTuple1);
//		System.out.println(inbox.size());
//
//		Object[] tuple = inbox.get(new FormalField(String.class),new FormalField(String.class));
//		System.out.println(tuple[0]);
//		//tuple = inbox.get(new FormalField(String.class));
//		System.out.println(tuple[1]);
//
//		System.out.println(inbox.size());
//		List<Object[]> tl = inbox.getAll(new ActualField("Number 2"));
//		System.out.println(inbox.size());
//
//		List<Object[]> t2 = inbox.getAll(new FormalField(Tuple.class));
//		System.out.println(inbox.size());

		/*
			Test 2:
		 */

		SpaceRepository repository = new SpaceRepository();
		Space TestSpace2 = new SequentialSpace();
		repository.add("TextSpace",TestSpace2);
		repository.addGate("tcp://localhost:25565/?conn");
		System.out.println("Size of reposTable before put : " + TestSpace2.size());

		Space RemoteSp = new RemoteSpace("tcp://localhost:25565/TextSpace?conn");
		RemoteSp.put("FORK", 2);
		System.out.println("Size of reposTable after put : " + TestSpace2.size());

		System.exit(0);
	}


}