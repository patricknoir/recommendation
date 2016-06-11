package org.patricknoir.recommendation

import cats.data._
import cats.std.all._
import cats.syntax.all._

/**
 * Created by patrick on 11/06/2016.
 */
trait AccountService[Account, Bet, Selection] {

  type Suggestions = Map[Selection, Double]

  type Error[A] = Xor[List[String], A]
  type Response[A] = Error[A]

  def account(accountNo: String): Response[Account]
  def bet(id: String): Response[Bet]
  def selection(id: String): Response[Selection]
  def selectionsForBet(bet: Bet): Response[Set[Selection]]
  def betsForSelection(selection: Selection): Response[Set[Bet]]
  def betsPerAccount(account: Account): Response[Set[Bet]]
  def accountsForBet(bet: Bet): Response[Set[Account]]

  def selectionsForAccount(account: Account): Response[Set[Selection]] = for {
    bets <- betsPerAccount(account)
    selections <- bets.map(selectionsForBet).reduce(_ |+| _)
  } yield selections

  def accountsWithSelection(selection: Selection): Response[Set[Account]] = for {
    bets <- betsForSelection(selection)
    accounts <- bets.map(accountsForBet).reduce(_ |+| _)
  } yield accounts

  def affinity(sels1: Set[Selection], sels2: Set[Selection]): Double =
    (sels1 intersect sels2).size.toDouble / (sels1 ++ sels2).size

  def suggest(mySels: Set[Selection], others: Set[Selection]): Suggestions = {
    val strength = affinity(mySels, others)
    (others -- mySels).map((_, strength)).toMap[Selection, Double]
  }

  def calculateSuggestions(mySels: Set[Selection], otherAccounts: Set[Account]): Response[Suggestions] = (
    for {
      account <- otherAccounts
      otherSelections = selectionsForAccount(account)
      suggestions = otherSelections.map(suggest(mySels, _))
    } yield suggestions
  ).reduce(_ |+| _)

  def recommend(account: Account, selection: Selection): Response[Suggestions] = for {
    mySelections <- selectionsForAccount(account)
    otherAccounts <- accountsWithSelection(selection)
    suggestions <- calculateSuggestions(mySelections, otherAccounts)
  } yield suggestions

}