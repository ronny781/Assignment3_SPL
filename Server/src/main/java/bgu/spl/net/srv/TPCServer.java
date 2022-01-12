package bgu.spl.net.srv;

import bgu.spl.net.impl.BGSEncoderDecoder;
import bgu.spl.net.impl.BGSProtocol;
import bgu.spl.net.impl.Database;

public class TPCServer {
    public static void main(String[] args) {
        Database db = Database.getInstance();//initialize Database
        if (args.length < 1) {
            System.out.println("You must supply port");
            return;
        }
        Server.threadPerClient(
                Integer.parseInt(args[0]), // port
                ()->new BGSProtocol(),
                ()->new BGSEncoderDecoder()
        ).serve();
    }
}
