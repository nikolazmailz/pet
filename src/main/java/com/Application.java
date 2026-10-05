package com;


/**
 * //1) написать простейший метод, который бы при работе бросил StackOverflowError
 * */

import java.util.*;

/**
 *
 * //2) написать свой класс, который бы реализовывал стек, с методами push, pop,  и peekMax,
 * который бы возвращал max Элемент в стеке за О(1)
 * */

/**
 * //3) Есть система, которая дает юзерам возможность работать с файлами в браузере.
 * //Стек стандартный: Java, Spring, ReactJS, Postgres. Файлы хранятся в файловой системе на бэке,
 * //метаданные файлов в БД. Команда реализовала фичу - переименование файла
 *
 *
 * @Transactional
 * public void process(String oldName, String newName) {
 *     Long id = exec("select id from file where name='" + oldName + "'"); //выполнение запроса к БД
 *     processFile(oldName, newName); //переименование файла на диске
 *     exec("update file set name='" + newName + "' where id = " + id); //выполнение запроса к БД
 * }
 *
 * */
public class Application {

    public static void main(String[] args) {

    }

    public class CustomStack<T extends Comparable<T>> {

        LinkedList<T> stack = new LinkedList<>();
        T max;

        public void push(T i){
            if(this.max == null) {
                this.max = i;
            } else {
                if(this.max.compareTo(i) < 0) {
                    this.max = i;
                }
            }
            stack.push(i);
        }

        public T pop() {
            T current = stack.pop();
            if(Objects.equals(current, this.max)) {
                this.max = stack.stream().max(T::compareTo).orElseGet(null);
            }
            return current;
        }

        public T peekMax(){
            return max;
        }
    }


}
