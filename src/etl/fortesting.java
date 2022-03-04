/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etl;

/**
 *
 * @author User
 */
public class fortesting {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
String str = "Hey this is Ram";
String [] words = str. split(" ");
for (String word : words)
if (word.contains("Ram")){
    System.out.println("THIS IS WHAT IT CONTAINS " + word.toString());
}

}
    }
    

