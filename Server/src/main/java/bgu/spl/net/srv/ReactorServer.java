package bgu.spl.net.srv;

import bgu.spl.net.impl.Database;

import bgu.spl.net.impl.BGSEncoderDecoder;
import bgu.spl.net.impl.BGSProtocol;

public class ReactorServer {
    public static void main(String[] args) {
        Database db = Database.getInstance();//initialize Database
        if (args.length < 2) {
            System.out.println("You must supply port and number of threads");
            return;
        }
        Server.reactor(
                Integer.parseInt(args[1]), // number of threads
                Integer.parseInt(args[0]), // port
                () -> new BGSProtocol(),
                () -> new BGSEncoderDecoder()
        ).serve();
    }
}
