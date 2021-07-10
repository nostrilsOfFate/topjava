package ru.javawebinar.topjava.util;

import ru.javawebinar.topjava.model.UserMeal;
import ru.javawebinar.topjava.model.UserMealWithExcess;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

public class UserMealsUtil {
    public static void main(String[] args) {
        List<UserMeal> meals = Arrays.asList(
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 30, 10, 0), "Завтрак", 500),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 30, 13, 0), "Обед", 1000),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 30, 20, 0), "Ужин", 500),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 0, 0), "Еда на граничное значение", 100),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 10, 0), "Завтрак", 1000),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 13, 0), "Обед", 500),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 20, 0), "Ужин", 410)
        );

        List<UserMealWithExcess> mealsTo = filteredByCycles(meals, LocalTime.of(7, 0), LocalTime.of(12, 0), 2000);
        //  mealsTo.forEach(System.out::println);

        System.out.println(filteredByStreams(meals, LocalTime.of(7, 0), LocalTime.of(12, 0), 2000));
    }

    public static List<UserMealWithExcess> filteredByCycles(List<UserMeal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        // TODO return filtered list with excess. Implement by cycles
        Set<UserMealWithExcess> RESULT = new HashSet<UserMealWithExcess>(); // создаем список еды результатной - из еды дня
        for (int i = 0; i < meals.size(); i++) { // для каждой еды дня проверяем весь лист БЕЗ ОПТИМИЗАЦИИ
            // часть получения списков на текущий день для Iго дня
            List<UserMeal> listAllMealInDay = new ArrayList<UserMeal>();//  получим список всей еды для данного дня
            List<UserMeal> listAllMealInDayBetweenTime = new ArrayList<UserMeal>();//  получим список всей еды для данного дня между датами
            for (UserMeal meal : meals) {
                if (meal.getDateTime().toLocalDate().isEqual(meals.get(i).getDateTime().toLocalDate())) { //дата для каждой еды в списке, конкретно проверку и выдачу еды делаем для каждой идивидуальной даты
                    listAllMealInDay.add(meal);
                    if (meal.getDateTime().toLocalTime().isAfter(startTime) && meal.getDateTime().toLocalTime().isBefore(endTime)) {
                        listAllMealInDayBetweenTime.add(meal);
                    }
                }
            }
            //часть поиска объема калорий на день
            int sumDayFood = 0;  // находит сумму калорий по факту за ВЕСЬ ДЕНЬ из всей еды в ВЕСЬ ДЕНЬ
            for (UserMeal userMeal : listAllMealInDay) {
                sumDayFood = sumDayFood + userMeal.getCalories();
            }
            //часть заполнения результата
            boolean excess = true;
            if (sumDayFood < caloriesPerDay) {
                excess = false;
            }
            for (UserMeal mealInDay : listAllMealInDayBetweenTime) { //Проставляем difference по результату сравнения sumEatenFood и caloriesPerDay
                RESULT.add(new UserMealWithExcess(mealInDay.getDateTime(), mealInDay.getDescription(), mealInDay.getCalories(), excess));
            }
        }
        return new ArrayList<>(RESULT);
    }

    public static List<UserMealWithExcess> filteredByStreamsSteamInSteam(List<UserMeal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        // TODO Implement by streams
        Set<UserMealWithExcess> RESULT = new HashSet<UserMealWithExcess>();
        Map<Integer, List<UserMeal>> foodEndThisList = new HashMap<>();
        meals.forEach(userMeal -> {
            LocalDate localDate = userMeal.getDateTime().toLocalDate();
            List<UserMeal> mealsPerDate = meals.stream()
                    .filter(userMeal1 -> userMeal1.getDateTime().toLocalDate().isEqual(localDate))
                    .collect(Collectors.toList());
            Integer sum = getSum(mealsPerDate);
            foodEndThisList.put(sum, mealsPerDate);
        });
        foodEndThisList.forEach((key, value) -> {
            boolean excess = key < caloriesPerDay;
            List<UserMealWithExcess> listFoodBetweenDates = value.stream()
                    .filter(meal -> meal.getDateTime().toLocalTime().isAfter(startTime) && meal.getDateTime().toLocalTime().isBefore(endTime))
                    .map(userMeal -> new UserMealWithExcess(userMeal.getDateTime(), userMeal.getDescription(), userMeal.getCalories(), excess))
                    .collect(Collectors.toList());
            RESULT.addAll(listFoodBetweenDates);
        });
        return new ArrayList<>(RESULT);
    }

    public static List<UserMealWithExcess> filteredByStreams(List<UserMeal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        // TODO Implement by streams
        return meals.stream()
                .sorted(Comparator.comparing(UserMeal::getDateTime))
                .map(userMeal -> {
                    List<UserMeal> mealsPerDate = getMealsPerDate(meals, userMeal);
                    Integer mealsSum = getSum(mealsPerDate);
                    return Map.entry(mealsSum, mealsPerDate);
                })
                .map(entry -> getUserMealsWithExcess(startTime, endTime, entry, caloriesPerDay))
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<UserMealWithExcess> filteredByStreamsForEachAndSteam(List<UserMeal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        // TODO Implement by streams
        Map<Integer, List<UserMeal>> foodEndThisList = new HashMap<>();
        meals.stream()
                .sorted(Comparator.comparing(UserMeal::getDateTime))
                .forEach(userMeal -> {
                    List<UserMeal> mealsPerDate = getMealsPerDate(meals, userMeal);
                    Integer sum = mealsPerDate.stream().mapToInt(UserMeal::getCalories).sum();
                    foodEndThisList.put(sum, mealsPerDate);
                });
        return foodEndThisList.entrySet().stream()
                .map(integerListEntry -> integerListEntry.getValue().stream()
                        .filter(meal -> meal.getDateTime().toLocalTime().isAfter(startTime) && meal.getDateTime().toLocalTime().isBefore(endTime))
                        .map(userMeal -> new UserMealWithExcess(userMeal.getDateTime(), userMeal.getDescription(), userMeal.getCalories(), integerListEntry.getKey() < caloriesPerDay))
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static List<UserMealWithExcess> getUserMealsWithExcess(LocalTime startTime, LocalTime endTime, Map.Entry<Integer, List<UserMeal>> entry, int caloriesPerDay) {
        return entry.getValue().stream()
                .filter(meal -> meal.getDateTime().toLocalTime().isAfter(startTime) && meal.getDateTime().toLocalTime().isBefore(endTime))
                .map(userMeal -> new UserMealWithExcess(userMeal.getDateTime(), userMeal.getDescription(), userMeal.getCalories(), entry.getKey() < caloriesPerDay))
                .collect(Collectors.toList());
    }

    private static int getSum(List<UserMeal> mealsPerDate) {
        return mealsPerDate.stream().mapToInt(UserMeal::getCalories).sum();
    }

    private static List<UserMeal> getMealsPerDate(List<UserMeal> meals, UserMeal userMeal) {
        return meals.stream()
                .filter(userMeal1 -> userMeal1.getDateTime().toLocalDate().isEqual(userMeal.getDateTime().toLocalDate()))
                .collect(Collectors.toList());
    }
}
