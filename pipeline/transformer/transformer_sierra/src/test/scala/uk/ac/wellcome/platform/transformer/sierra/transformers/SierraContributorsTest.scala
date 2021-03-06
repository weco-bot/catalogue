package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  VarField
}
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}

class SierraContributorsTest
    extends AnyFunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {

  it("gets an empty contributor list from empty bib data") {
    transformAndCheckContributors(
      varFields = List(),
      expectedContributors = List())
  }

  it("extracts a mixture of Person, Organisation and Meeting contributors") {
    val varFields = List(
      createVarFieldWith(
        marcTag = "100",
        subfields = List(
          MarcSubfield(tag = "a", content = "Sarah the soybean")
        )
      ),
      createVarFieldWith(
        marcTag = "100",
        subfields = List(
          MarcSubfield(tag = "a", content = "Sam the squash,"),
          MarcSubfield(tag = "c", content = "Sir")
        )
      ),
      createVarFieldWith(
        marcTag = "110",
        subfields = List(
          MarcSubfield(tag = "a", content = "Spinach Solicitors")
        )
      ),
      createVarFieldWith(
        marcTag = "700",
        subfields = List(
          MarcSubfield(tag = "a", content = "Sebastian the sugarsnap")
        )
      ),
      createVarFieldWith(
        marcTag = "710",
        subfields = List(
          MarcSubfield(tag = "a", content = "Shallot Swimmers")
        )
      ),
      createVarFieldWith(
        marcTag = "711",
        subfields = List(
          MarcSubfield(tag = "a", content = "Sammys meet the Sammys"),
          MarcSubfield(tag = "c", content = "at Sammys")
        )
      ),
    )

    val expectedContributors = List(
      Contributor(Person("Sarah the soybean"), roles = Nil),
      Contributor(Person("Sam the squash, Sir"), roles = Nil),
      Contributor(Organisation("Spinach Solicitors"), roles = Nil),
      Contributor(Person("Sebastian the sugarsnap"), roles = Nil),
      Contributor(Organisation("Shallot Swimmers"), roles = Nil),
      Contributor(Meeting("Sammys meet the Sammys at Sammys"), roles = Nil)
    )
    transformAndCheckContributors(
      varFields = varFields,
      expectedContributors = expectedContributors)
  }

  describe("Person") {
    it("extracts and combines only subfields $$a $$b $$c $$d for the label") {
      // Based on https://search.wellcomelibrary.org/iii/encore/record/C__Rb1795764?lang=eng
      // as retrieved on 25 April 2019.

      val name = "Charles Emmanuel"
      val numeration = "III,"
      val titlesAndOtherWords = "King of Sardinia,"
      val dates = "1701-1773,"
      val varField100 = createVarFieldWith(
        marcTag = "100",
        subfields = List(
          MarcSubfield(tag = "a", content = name),
          MarcSubfield(tag = "b", content = numeration),
          MarcSubfield(tag = "c", content = titlesAndOtherWords),
          MarcSubfield(tag = "d", content = dates),
        )
      )
      val varField700 = varField100.copy(marcTag = Some("700"))
      val varFields = List(varField100, varField700)

      val expectedContributors = List(
        Contributor(
          Person(label = "Charles Emmanuel III, King of Sardinia, 1701-1773"),
          roles = Nil),
        Contributor(
          Person(label = "Charles Emmanuel III, King of Sardinia, 1701-1773"),
          roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it(
      "combines subfield $$t with $$a $$b $$c $$d and creates an Agent, not a Person from MARC field 100 / 700") {
      // Based on https://search.wellcomelibrary.org/iii/encore/record/C__Rb1159639?marcData=Y
      // as retrieved on 4 February 2019.
      val varFields = List(
        createVarFieldWith(
          marcTag = "700",
          subfields = List(
            MarcSubfield(tag = "a", content = "Shakespeare, William,"),
            MarcSubfield(tag = "d", content = "1564-1616."),
            MarcSubfield(tag = "t", content = "Hamlet.")
          )
        )
      )

      val bibData = createSierraBibDataWith(varFields = varFields)
      val contributors = SierraContributors(createSierraBibNumber, bibData)
      contributors should have size 1
      val contributor = contributors.head

      contributor.agent shouldBe Agent(
        "Shakespeare, William, 1564-1616. Hamlet.")
    }

    it(
      "gets the name from MARC tags 100 and 700 subfield $$a in the right order") {
      val name1 = "Alfie the Artichoke"
      val name2 = "Alison the Apple"
      val name3 = "Archie the Aubergine"

      // The correct ordering is "everything from 100 first, then 700", and
      // we deliberately pick an ordering that's different from that for
      // the MARC fields, so we can check it really is applying this rule.
      val varFields = List(
        createVarFieldWith(
          marcTag = "700",
          subfields = List(MarcSubfield(tag = "a", content = name2))
        ),
        createVarFieldWith(
          marcTag = "100",
          subfields = List(MarcSubfield(tag = "a", content = name1))
        ),
        createVarFieldWith(
          marcTag = "700",
          subfields = List(MarcSubfield(tag = "a", content = name3))
        )
      )

      val expectedContributors = List(
        Contributor(Person(label = name1), roles = Nil),
        Contributor(Person(label = name2), roles = Nil),
        Contributor(Person(label = name3), roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets the roles from subfield $$e") {
      val name = "Violet the Vanilla"
      val role1 = "spice"
      val role2 = "flavour"

      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "e", content = role1),
            MarcSubfield(tag = "e", content = role2)
          )
        )
      )

      val expectedContributors = List(
        Contributor(
          agent = Person(label = name),
          roles = List(ContributionRole(role1), ContributionRole(role2))
        )
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets an identifier from subfield $$0") {
      val name = "Ivan the ivy"
      val lcshCode = "lcsh7101607"

      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = lcshCode)
          )
        )
      )

      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("lc-names"),
        ontologyType = "Person",
        value = lcshCode
      )

      val expectedContributors = List(
        Contributor(
          Person(label = name, id = Identifiable(sourceIdentifier)),
          roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it(
      "combines identifiers with inconsistent spacing/punctuation from subfield $$0") {
      val name = "Wanda the watercress"
      val lcshCodeCanonical = "lcsh2055034"
      val lcshCode1 = "lcsh 2055034"
      val lcshCode2 = "  lcsh2055034 "
      val lcshCode3 = " lc sh 2055034"

      // Based on an example from a real record; see Sierra b3017492.
      val lcshCode4 = "lcsh 2055034.,"

      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = lcshCode1),
            MarcSubfield(tag = "0", content = lcshCode2),
            MarcSubfield(tag = "0", content = lcshCode3),
            MarcSubfield(tag = "0", content = lcshCode4)
          )
        )
      )

      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("lc-names"),
        ontologyType = "Person",
        value = lcshCodeCanonical
      )

      val expectedContributors = List(
        Contributor(
          Person(label = name, id = Identifiable(sourceIdentifier)),
          roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it(
      "does not identify the contributor if there are multiple distinct identifiers in subfield $$0") {
      val name = "Darren the Dill"
      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = "lcsh9069541"),
            MarcSubfield(tag = "0", content = "lcsh3384149")
          )
        )
      )

      val expectedContributors = List(
        Contributor(Person(name), roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("normalises Person contributor labels") {
      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(MarcSubfield(tag = "a", content = "George,"))
        ),
        createVarFieldWith(
          marcTag = "700",
          subfields = List(
            MarcSubfield(tag = "a", content = "Sebastian,")
          )
        )
      )

      val expectedContributors = List(
        Contributor(Person(label = "George"), roles = Nil),
        Contributor(Person(label = "Sebastian"), roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }
  }

  describe("Organisation") {
    it("gets the name from MARC tag 110 subfield $$a") {
      val name = "Ona the orache"

      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields = List(MarcSubfield(tag = "a", content = name))
        )
      )

      val expectedContributors = List(
        Contributor(Organisation(label = name), roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it(
      "combines only subfields $$a $$b $$c $$d (multiples of) with spaces from MARC field 110 / 710") {
      // Based on https://search.wellcomelibrary.org/iii/encore/record/C__Rb1000984
      // as retrieved from 25 April 2019
      val name =
        "IARC Working Group on the Evaluation of the Carcinogenic Risk of Chemicals to Man."
      val subordinateUnit = "Meeting"
      val date = "1972 :"
      val place = "Lyon, France"

      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "b", content = subordinateUnit),
            MarcSubfield(tag = "d", content = date),
            MarcSubfield(tag = "c", content = place),
            MarcSubfield(tag = "n", content = "  79125097")
          )
        )
      )

      val expectedContributors = List(
        Contributor(
          Organisation(
            label =
              "IARC Working Group on the Evaluation of the Carcinogenic Risk of Chemicals to Man. Meeting 1972 : Lyon, France"
          ),
          roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it(
      "gets the name from MARC tags 110 and 710 subfield $$a in the right order") {
      val name1 = "Mary the mallow"
      val name2 = "Mike the mashua"
      val name3 = "Mickey the mozuku"

      // The correct ordering is "everything from 110 first, then 710", and
      // we deliberately pick an ordering that's different from that for
      // the MARC fields, so we can check it really is applying this rule.
      val varFields = List(
        createVarFieldWith(
          marcTag = "710",
          subfields = List(MarcSubfield(tag = "a", content = name2))
        ),
        createVarFieldWith(
          marcTag = "110",
          subfields = List(MarcSubfield(tag = "a", content = name1))
        ),
        createVarFieldWith(
          marcTag = "710",
          subfields = List(MarcSubfield(tag = "a", content = name3))
        )
      )

      val expectedContributors = List(
        Contributor(Organisation(label = name1), roles = Nil),
        Contributor(Organisation(label = name2), roles = Nil),
        Contributor(Organisation(label = name3), roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets the roles from subfield $$e") {
      val name = "Terry the turmeric"
      val role1 = "dye"
      val role2 = "colouring"

      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "e", content = role1),
            MarcSubfield(tag = "e", content = role2)
          )
        )
      )

      val expectedContributors = List(
        Contributor(
          Organisation(label = name),
          roles = List(ContributionRole(role1), ContributionRole(role2))
        )
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets an identifier from subfield $$0") {
      val name = "Gerry the Garlic"
      val lcshCode = "lcsh7212"

      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = lcshCode)
          )
        )
      )

      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("lc-names"),
        ontologyType = "Organisation",
        value = lcshCode
      )

      val expectedContributors = List(
        Contributor(
          Organisation(label = name, id = Identifiable(sourceIdentifier)),
          roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets an identifier with inconsistent spacing from subfield $$0") {
      val name = "Charlie the chive"
      val lcshCodeCanonical = "lcsh6791210"
      val lcshCode1 = "lcsh 6791210"
      val lcshCode2 = "  lcsh6791210 "
      val lcshCode3 = " lc sh 6791210"

      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = lcshCode1),
            MarcSubfield(tag = "0", content = lcshCode2),
            MarcSubfield(tag = "0", content = lcshCode3)
          )
        )
      )

      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("lc-names"),
        ontologyType = "Organisation",
        value = lcshCodeCanonical
      )

      val expectedContributors = List(
        Contributor(
          Organisation(label = name, id = Identifiable(sourceIdentifier)),
          roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it(
      "does not identify the contributor if there are multiple distinct identifiers in subfield $$0") {
      val name = "Luke the lime"
      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = "lcsh3349285"),
            MarcSubfield(tag = "0", content = "lcsh9059917")
          )
        )
      )

      val expectedContributors = List(
        Contributor(Organisation(label = name), roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("normalises Organisation contributor labels") {
      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields =
            List(MarcSubfield(tag = "a", content = "The organisation,"))
        ),
        createVarFieldWith(
          marcTag = "710",
          subfields =
            List(MarcSubfield(tag = "a", content = "Another organisation,"))
        )
      )

      val expectedContributors = List(
        Contributor(Organisation(label = "The organisation"), roles = Nil),
        Contributor(Organisation(label = "Another organisation"), roles = Nil)
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }
  }

  // This is based on transformer failures we saw in October 2018 --
  // records 3069865, 3069866, 3069867, 3069872 all had empty instances of
  // the 110 field.
  it("returns an empty list if subfield $$a is missing") {
    val varFields = List(
      createVarFieldWith(
        marcTag = "100",
        subfields = List(
          MarcSubfield(tag = "e", content = "")
        )
      )
    )

    transformAndCheckContributors(
      varFields = varFields,
      expectedContributors = List())
  }

  describe("Meeting") {
    it("gets the name from MARC tag 111 subfield $$a") {
      val varField = createVarFieldWith(
        marcTag = "111",
        subfields = List(MarcSubfield(tag = "a", content = "Big meeting"))
      )
      val contributor = Contributor(Meeting(label = "Big meeting"), roles = Nil)
      transformAndCheckContributors(List(varField), List(contributor))
    }

    it("gets the name from MARC tag 711 subfield $$a") {
      val varField = createVarFieldWith(
        marcTag = "711",
        subfields = List(MarcSubfield(tag = "a", content = "Big meeting"))
      )
      val contributor = Contributor(Meeting(label = "Big meeting"), roles = Nil)
      transformAndCheckContributors(List(varField), List(contributor))
    }

    it("combinies subfields $$a, $$c, $$d and $$t with spaces") {
      val varField = createVarFieldWith(
        marcTag = "111",
        subfields = List(
          MarcSubfield(tag = "a", content = "1"),
          MarcSubfield(tag = "b", content = "not used"),
          MarcSubfield(tag = "c", content = "2"),
          MarcSubfield(tag = "d", content = "3"),
          MarcSubfield(tag = "t", content = "4"),
        )
      )
      val contributor = Contributor(Meeting(label = "1 2 3 4"), roles = Nil)
      transformAndCheckContributors(List(varField), List(contributor))
    }

    it("gets the roles from subfield $$j") {
      val varField = createVarFieldWith(
        marcTag = "111",
        subfields = List(
          MarcSubfield(tag = "a", content = "label"),
          MarcSubfield(tag = "e", content = "not a role"),
          MarcSubfield(tag = "j", content = "1st role"),
          MarcSubfield(tag = "j", content = "2nd role"),
        )
      )
      val contributor = Contributor(
        agent = Meeting(label = "label"),
        roles = List(ContributionRole("1st role"), ContributionRole("2nd role"))
      )
      transformAndCheckContributors(List(varField), List(contributor))
    }

    it("gets an identifier from subfield $$0") {
      val varField = createVarFieldWith(
        marcTag = "111",
        subfields = List(
          MarcSubfield(tag = "a", content = "label"),
          MarcSubfield(tag = "0", content = "456")
        )
      )
      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("lc-names"),
        ontologyType = "Meeting",
        value = "456"
      )
      val contributor = Contributor(
        Meeting(label = "label", id = Identifiable(sourceIdentifier)),
        roles = Nil
      )
      transformAndCheckContributors(List(varField), List(contributor))
    }
  }

  private def transformAndCheckContributors(
    varFields: List[VarField],
    expectedContributors: List[Contributor[Unminted]]
  ) = {
    val bibId = createSierraBibNumber
    val bibData = createSierraBibDataWith(varFields = varFields)
    SierraContributors(bibId, bibData) shouldBe expectedContributors
  }
}
