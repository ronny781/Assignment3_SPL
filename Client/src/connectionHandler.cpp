#include "../include/connectionHandler.h"
#include <vector>
#include <iomanip>
#include <sstream>

using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;


ConnectionHandler::ConnectionHandler(string host, short port) : host_(host), port_(port), io_service_(),
                                                                socket_(io_service_) {}

ConnectionHandler::~ConnectionHandler() {
    close();
}

bool ConnectionHandler::connect() {
    std::cout << "Starting connect to "
              << host_ << ":" << port_ << std::endl;
    try {
        tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
        boost::system::error_code error;
        socket_.connect(endpoint, error);
        if (error)
            throw boost::system::system_error(error);
    }
    catch (std::exception &e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
    size_t tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToRead > tmp) {
            tmp += socket_.read_some(boost::asio::buffer(bytes + tmp, bytesToRead - tmp), error);
        }
        if (error)
            throw boost::system::system_error(error);
    } catch (std::exception &e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
    int tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToWrite > tmp) {
            tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
        }
        if (error)
            throw boost::system::system_error(error);
    } catch (std::exception &e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}


bool ConnectionHandler::sendLine(std::string &line) {
    unsigned int index = line.find(" "); //find first space
    string strOpCode = line; // string with op code

    if (index != string::npos) {// a space was found
        strOpCode = line.substr(0, index); // get substring with op code
        line = line.substr(index + 1, line.length() - index); //remove op code from rest of the line
    }

    short opCode = stringToOpCode(strOpCode); // get op code
    if (opCode == 3 || opCode == 7) line = "";
    if (opCode == 1 || opCode == 2 || opCode == 4 || opCode == 5 || opCode == 6 || opCode == 8 || opCode == 12 ||
        opCode == 3 || opCode == 7) {

        if (opCode != 8 && opCode != 4)
            std::replace(line.begin(), line.end(), ' ', '\0'); //Replace spaces with \0
        if (opCode != 2 && opCode != 3 && opCode != 7)
            line += '\0'; // add ending character

        if (opCode == 6)
            line += generateCurrentTime() + '\0';
        line += ';';
        const char *strBytes = line.c_str(); // bytes array of the strings
        unsigned messageLength = 2 + line.length(); // full length of message with op code
        char messageBytes[messageLength]; // the full bytes array to send
        shortToBytes(opCode, messageBytes); // put opcode as first 2 bytes
        for (unsigned int i = 2; i < messageLength; i++) {
            messageBytes[i] = strBytes[i - 2];
        }
        bool result = sendBytes(messageBytes, messageLength); // send full message
        if (!result) return false;
    }

    return true;

}

short ConnectionHandler::stringToOpCode(const std::string &opCode) {
    if (opCode == "REGISTER") return 1;
    if (opCode == "LOGIN") return 2;
    if (opCode == "LOGOUT") return 3;
    if (opCode == "FOLLOW") return 4;
    if (opCode == "POST") return 5;
    if (opCode == "PM") return 6;
    if (opCode == "LOGSTAT") return 7;
    if (opCode == "STAT") return 8;
    if (opCode == "NOTIFICATION") return 9;
    if (opCode == "ACK") return 10;
    if (opCode == "ERROR") return 11;
    if (opCode == "BLOCK") return 12;
    return 0;
}

string ConnectionHandler::generateCurrentTime() {
    auto t = std::time(nullptr);
    auto tm = *std::localtime(&t);

    std::ostringstream oss;
    oss << std::put_time(&tm, "%d-%m-%Y");
    auto str = oss.str();
    return str;
}


bool ConnectionHandler::getLine(std::string &line) {
    char ch; // current byte
    std::vector<char> bytes; //current bytes received by server
    short opCode = 0, messageCode = 0;
    char opCodeBytes[2]; // temp array to get opcode
    char messageCodeBytes[2]; // temp array to get message op code
    try {
        do {
            if (!getBytes(&ch, 1))
                return false; // problem reading bytes
            bytes.push_back(ch); // store the received bytes one by one

        } while (ch != ';'); //first 4 bytes may contain '\0' byte, check for ACK message end condition
    } catch (std::exception &e) {
        std::cerr << "recv failed2 (Error: " << e.what() << ')' << std::endl;
        return false;
    }

    opCodeBytes[0] = bytes[0];
    opCodeBytes[1] = bytes[1];
    opCode = bytesToShort(opCodeBytes); // get opCode as short

    if (opCode != 9) { //Then its ACK or Error
        messageCodeBytes[0] = bytes[2];
        messageCodeBytes[1] = bytes[3];
        messageCode = bytesToShort(messageCodeBytes); // get message opCode as short

        if (opCode == 10) {// ACK message
            line = "ACK " + std::to_string(messageCode);
            if (bytes.size() > 5) { // optional message was received(This happens only in LOGSTAT or STAT?)
                line += ' '; // Added this
                for (unsigned i = 4; i < bytes.size() - 1; i++) {// first 4 bytes reserved for op codes
                    line.append(1, bytes[i]); // ACK message additional string
                    if (bytes[i] == '\n' && i != bytes.size() - 2)
                        line.append("ACK " + std::to_string(messageCode) + " ");
                }
                line.pop_back(); // remove the last "\n"
            }
        } else if (opCode == 11) {// Error message
            line = "ERROR " + std::to_string(messageCode);
        }

    } else { //Notification
        char type = bytes[2];
        line += "NOTIFICATION ";
        if (type == 0)
            line += "PM ";
        else
            line += "Public ";

        for (unsigned i = 3; i < bytes.size() - 1; i++)
            if (bytes[i] != '\0')
                line.append(1, bytes[i]);
            else
                line.append(1, ' ');

    }

    return true;
}

short ConnectionHandler::bytesToShort(const char *bytesArr) {
    short result = (short) ((bytesArr[0] & 0xff) << 8);
    result += (short) (bytesArr[1] & 0xff);
    return result;
}

void ConnectionHandler::shortToBytes(short num, char *bytesArr) {
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}


// Close down the connection properly.
void ConnectionHandler::close() {
    try {
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
}