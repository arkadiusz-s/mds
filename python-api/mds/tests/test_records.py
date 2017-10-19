# -*- coding: utf-8 -*-
"""
Managed Data Structures
Copyright © 2017 Hewlett Packard Enterprise Development Company LP.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

As an exception, the copyright holders of this Library grant you permission
to (i) compile an Application with the Library, and (ii) distribute the 
Application containing code generated by the Library and added to the 
Application during this compilation process under terms of your choice, 
provided you also meet the terms and conditions of the Application license.
"""

import unittest

import mds
from mds.managed import Record

class ExampleRecord(Record, ident="PythonTest::ExampleRecord"):  # TODO => Test
    """
    Descendents of Record should explictly declare an `ident` key/value pair in the
    class descriptor as above; this is how the type will be known to MDS.
    """

    @staticmethod
    def schema() -> dict:
        """
        This method *must* be overridden in Record-derived classes, and follow the
        following syntax for declaring the record schema.

        Once the class `cls` has come into scope, an instantiated copy of this object
        will be available via cls.type_decl
        """
        return {
            "is_active": Record.declare_const_field(mds.typing.bool),
            "number_of_players": Record.declare_field(mds.typing.ushort)
        }


class SimpleRecord(Record, ident="schema_SimpleRecords"):

    @staticmethod
    def schema() -> dict:
        return {
            "is_active": Record.declare_const_field(mds.typing.bool)
        }


class LessSimpleRecord(Record, ident="schema_LessSimpleRecord"):

    @staticmethod
    def schema() -> dict:
        return {
            "is_active": Record.declare_const_field(mds.typing.bool),
            "numerator": Record.declare_field(mds.typing.float),
            "denominator": Record.declare_field(mds.typing.double)
        }


class ComplexRecord(Record, ident="schema_ComplexRecord"):

    def __init__(self):
        self.python_field = True

        # Remember to call the super constructor before acccessing MDS fields
        super().__init__()

    @staticmethod
    def schema() -> dict:
        return {
            "is_active": Record.declare_const_field(mds.typing.bool),
            "numerator": Record.declare_field(mds.typing.float),
            "denominator": Record.declare_field(mds.typing.double)
        }


class TestRecords(unittest.TestCase):

    RECORDS = [SimpleRecord, LessSimpleRecord, ComplexRecord]

    def __create_and_test(self, cls):
        record = cls()
        self.assertIs(type(record), cls)
        self.assertIsInstance(record, Record)
        return record

    def test_can_make_simple(self):
        record = self.__create_and_test(SimpleRecord)
        field = "is_active"

        self.assertEqual(record.is_active, True)

    @unittest.skip("Debugging")
    def test_can_make_less_simple(self):
        self.__create_and_test(LessSimpleRecord)

    @unittest.skip("Debugging")
    def test_can_make_complex(self):
        self.__create_and_test(ComplexRecord)

    def test_can_bind_to_namespace(self):
        pass

    def test_can_retrieve_from_namespace(self):
        pass

    def test_forwarding_resolution(self):
        pass


if __name__ == '__main__':
    unittest.main()
