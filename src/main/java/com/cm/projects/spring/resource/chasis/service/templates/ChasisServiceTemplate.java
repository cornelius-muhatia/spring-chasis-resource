package com.cm.projects.spring.resource.chasis.service.templates;

import com.cm.projects.spring.resource.chasis.annotations.*;
import com.cm.projects.spring.resource.chasis.exceptions.ExpectationFailed;
import com.cm.projects.spring.resource.chasis.exceptions.GeneralBadRequest;
import com.cm.projects.spring.resource.chasis.exceptions.NotFoundException;
import com.cm.projects.spring.resource.chasis.service.ChasisService;
import com.cm.projects.spring.resource.chasis.utils.AppConstants;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.criteria.*;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Cornelius M.
 * @version 1.0.0
 * @see ChasisService
 * <b>Additional methods include: <br /></b>
 * <ul>
 * <li>{@link ChasisServiceTemplate#validateManyToOne(Field, PropertyAccessor, Class)}</li>
 * <li>{@link ChasisServiceTemplate#getEntityId(Object)}</li>
 * <li>{@link ChasisServiceTemplate#instantiateEntity(Object, Class)}</li>
 * </ul>
 */
public class ChasisServiceTemplate implements ChasisService {

    /**
     * Entity manager
     */
    private final EntityManager entityManager;
    /**
     * Logger
     */
    private Logger log = LoggerFactory.getLogger(this.getClass());

    public ChasisServiceTemplate(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Used to check if @{@link ManyToOne}field exists in the repository
     *
     * @param field    Field to validated
     * @param accessor Bean wrapper accessor
     * @param clazz    Parent entity class
     * @throws NotFoundException if the relation entity doesn't exist in the repository
     */
    private void validateManyToOne(Field field, PropertyAccessor accessor, Class clazz) throws NotFoundException, ExpectationFailed {

        if (field.isAnnotationPresent(ManyToOne.class)) {
            var relEntity = accessor.getPropertyValue(field.getName());
            if (relEntity != null) {
                NickName nickName = relEntity.getClass().getDeclaredAnnotation(NickName.class);
                PropertyAccessor relAccessor = PropertyAccessorFactory.forBeanPropertyAccess(relEntity);
                Object id = null;
                //check if entity exists
                for (Field f2 : relEntity.getClass().getDeclaredFields()) {
                    if (f2.isAnnotationPresent(Id.class)) {
                        id = relAccessor.getPropertyValue(f2.getName());
                        relEntity = entityManager.find(relEntity.getClass(), id);
                        if (relEntity == null) {
                            if (nickName != null) {
                                throw new NotFoundException(nickName.name() + " with id " + id + " doesn't exist");
                            } else {
                                throw new NotFoundException("Record with id " + id + " doesn't exist");
                            }
                        } else {
                            //initialise accessor again to reflect database entity
                            relAccessor = PropertyAccessorFactory.forBeanPropertyAccess(relEntity);
                        }
                    }
                }
                if (relAccessor.isReadableProperty("status")) {//check if records is active
                    Object status = relAccessor.getPropertyValue("status");
                    try {
                        Short statusId = null;
                        if (status instanceof Number) {
                            statusId = Short.valueOf(status + "");
                        } else {
                            for (Field f : status.getClass().getDeclaredFields()) {
                                if (f.isAnnotationPresent(Id.class)) {
                                    PropertyAccessor sAccessor = PropertyAccessorFactory.forBeanPropertyAccess(status);
                                    statusId = Short.valueOf(sAccessor.getPropertyValue(f.getName()) + "");
                                    break;
                                }
                            }
                        }
                        if (statusId != null && !AppConstants.STATUS_ID_ACTIVE.equals(statusId)) {
                            throw new ExpectationFailed(((nickName == null) ? "Record" : nickName.name()) + " with id " + id + " is not active");
                        }
                    } catch (NullPointerException e) {
                        log.error("Failed to instatiate status field. Skipping active status validation", e);
                    }

                }
            }
        }
    }

    @Override
    public void validateRelEntities(Object t, PropertyAccessor accessor) throws NotFoundException, ExpectationFailed {

        Field[] fields = t.getClass().getDeclaredFields();
        int iLow = 0;
        int iMax = fields.length - 1;

        while (iLow < iMax) {
            //validate many to ome collections
            this.validateManyToOne(fields[iLow], accessor, t.getClass());
            this.validateManyToOne(fields[iMax], accessor, t.getClass());
            iLow++;
            iMax--;
        }
    }

    @Override
    public void validateUniqueFields(BeanWrapper accessor) throws GeneralBadRequest {
        //Declare properties
//        PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(t);
        log.debug("Checking for unique fields on entity {}", accessor.getWrappedInstance());
        String fieldId = null;
        Object id = null;
        boolean hasIntrash = false;
        List<Field> uniqueFields = new ArrayList<>();
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();

        //Retrieve annotated fields and values
        for (Field field : accessor.getWrappedClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Unique.class)) {
                uniqueFields.add(field);
            }
            if (field.isAnnotationPresent(Id.class)) {
                fieldId = field.getName();
                id = accessor.getPropertyValue(field.getName());
            }
            if (field.getName().equalsIgnoreCase("intrash")) {
                hasIntrash = true;
            }
            if(field.isAnnotationPresent(OneToMany.class)){//validate collection for unique fields
                Collection<?> relEntities = (Collection<?>) accessor.getPropertyValue(field.getName());
                log.debug("Found OneToMany collection {} checking for unique fields", relEntities);
                if(relEntities != null){
                    this.validateUniqueFields(relEntities);
                }
            }
        }

        //check if id field is present
        if (fieldId == null) {
            throw new RuntimeException("Failed to validate unique fields. Entity doesn't have an id field");
        }

        //validate unique fields
        for (Field field : uniqueFields) {
            CriteriaQuery criteriaQuery = criteriaBuilder.createQuery(accessor.getWrappedClass());
            Root root = criteriaQuery.from(accessor.getWrappedClass());
            Unique unique = field.getDeclaredAnnotation(Unique.class);
            Object value = accessor.getPropertyValue(field.getName());

            if (hasIntrash) {
                if (id == null) {
                    criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.and(criteriaBuilder.equal(root.get(field.getName()), value),
                            criteriaBuilder.equal(root.get("intrash"), AppConstants.NO))));
                } else {
                    criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.and(criteriaBuilder.equal(root.get(field.getName()), value),
                            criteriaBuilder.equal(root.get("intrash"), AppConstants.NO)), criteriaBuilder.notEqual(root.get(fieldId), id)));
                }
            } else {
                if (id == null) {
                    criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.equal(root.get(field.getName()), value)));
                } else {
                    criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.equal(root.get(field.getName()), value), criteriaBuilder.notEqual(root.get(fieldId), id)));
                }
            }
            if (!this.entityManager.createQuery(criteriaQuery).getResultList().isEmpty()) {
                throw new GeneralBadRequest("Record with similar " + unique.fieldName() + " exists", HttpStatus.CONFLICT);
            }
        }

        //validate unique compound fields
        if (accessor.getWrappedClass().isAnnotationPresent(CompoundUnique.class)) {
            for (UniquePair pair : accessor.getWrappedClass().getAnnotation(CompoundUnique.class).pairs()) {
//                Serializable id = this.getEntityId(t)
                CriteriaQuery criteriaQuery = criteriaBuilder.createQuery(accessor.getWrappedClass());
                Root root = criteriaQuery.from(accessor.getWrappedClass());
                ArrayList<Predicate> predicates = new ArrayList<>();
                for (String fieldName : pair.fields()) {
                    Object value = accessor.getPropertyValue(fieldName);
                    predicates.add(criteriaBuilder.equal(root.get(fieldName), value));

                }
                //if is an existing record skip the record in the database
                if (id != null) {
                    predicates.add(criteriaBuilder.notEqual(root.get(fieldId), id));
                }

                //create intrash criteria
                if (hasIntrash) {
                        predicates.add(criteriaBuilder.notEqual(root.get("intrash"), AppConstants.YES));
                }
                criteriaQuery.where(predicates.toArray(Predicate[]::new));
                if (!this.entityManager.createQuery(criteriaQuery).getResultList().isEmpty()) {
                    throw new GeneralBadRequest("Record with similar " + pair.constraintName() + " exists", HttpStatus.CONFLICT);
                }

            }
        }
    }

    @Override
    public void validateUniqueFields(Collection<?> t) throws GeneralBadRequest {
        for(Object record : t){
            this.validateUniqueFields(PropertyAccessorFactory.forBeanPropertyAccess(record));
        }
    }

    @Override
    public <T> Object instantiateEntity(T id, Class<?> type) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        log.debug("Instantiate new entity of type {} using id {}", type, id);
        if (type == null) {
            return null;
        }
        Constructor constructor = type.getDeclaredConstructor(id.getClass());
        return constructor.newInstance(id);
    }

    @Override
    public Serializable getEntityId(Object entity) {
        BeanWrapper accessor = PropertyAccessorFactory.forBeanPropertyAccess(entity);
        for (Field f : entity.getClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class)) {
                return (Serializable) accessor.getPropertyValue(f.getName());
            }
        }
        return null;
    }

    @Override
    public Date tryParse(String dateString) throws ParseException {
        List<String> formatStrings = Arrays.asList("dd/MM/yyyy", "dd/MM/yyyy HH:mm:ss.SSS",
                "dd/MM/yyyy HH:mm:ss", "dd-MM-yyyy", "dd-MM-yyyy HH:mm:ss.SSS", "dd-MM-yyyy HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss");
        for (String formatString : formatStrings) {
            try {
                return new SimpleDateFormat(formatString).parse(dateString);
            }catch(ParseException ex){
                log.debug("Failed to parse string {} to date using format {}", dateString, formatString);
            }
        }
        throw new ParseException("Failed to parse string " + dateString + " to date", 1);
    }

    @Override
    public <T> Stream<T> findAll(Pageable pg, Class<T> clazz, HttpServletRequest request, EntityManager entityManager) throws ParseException {
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<T> root = criteriaQuery.from(clazz);
        ArrayList<Predicate> searchPreds = new ArrayList<>();
        ArrayList<Predicate> filterPreds = new ArrayList<>();
        List<Order> ords = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        //retrieve filter and search params
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Searchable.class) && request.getParameter("needle") != null) { //process search attributes
                searchPreds.add(criteriaBuilder.like(criteriaBuilder.upper(root.get(field.getName())),
                        "%" + request.getParameter("needle").toUpperCase() + "%"));
            }

            if (field.isAnnotationPresent(Filter.class)) {//process filter attributes
                if (field.getAnnotation(Filter.class).isDateRange() && request.getParameter("to") != null
                        && request.getParameter("from") != null) {//filter date range

                    Date from = this.tryParse(request.getParameter("from"));
                    cal.setTime(from);
                    cal.add(Calendar.DAY_OF_WEEK, -1);
                    from = cal.getTime();

                    Date to = this.tryParse(request.getParameter("to"));
                    cal.setTime(to);
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                    to = cal.getTime();
                    Predicate datePred = criteriaBuilder.between(root.get(field.getName()).as(Date.class), from, to);
                    filterPreds.add(datePred);
                } else if (request.getParameter(field.getName()) != null && !request.getParameter(field.getName()).isEmpty()) {
                    if (field.isAnnotationPresent(ManyToOne.class)) {
                        BeanWrapper wrapper = new BeanWrapperImpl(field.getType());
                        for (Field f : field.getType().getDeclaredFields()) {
                            if (f.isAnnotationPresent(Id.class)) {
                                wrapper.setPropertyValue(f.getName(), request.getParameter(field.getName()));
                                break;
                            }
                        }
                        filterPreds.add(criteriaBuilder.equal(root.get(field.getName()), wrapper.getWrappedInstance()));
                    } else {
                        filterPreds.add(criteriaBuilder.like(root.get(field.getName()).as(String.class),
                                request.getParameter(field.getName())));
                    }
                } else {
                    log.debug("Failed to find parameter {} skipping filtering for field {} ", field.getName(), field.getName());
                }
            }
            //check if order parameter is available for the current field
            Sort.Order ord = pg.getSort().getOrderFor(field.getName());
            if (ord != null) {
                log.debug("Found ordering paramater ({}) for field {} preparing ordering query", ord, field.getName());
                if (ord.isAscending()) {
                    ords.add(criteriaBuilder.asc(root.get(field.getName())));
                } else {
                    ords.add(criteriaBuilder.desc(root.get(field.getName())));
                }
            }
            //check if is intrash
            if (field.getName().equalsIgnoreCase("intrash")) {
                filterPreds.add(criteriaBuilder.equal(root.get(field.getName()), AppConstants.NO));
            }
        }
        if (!filterPreds.isEmpty() && !searchPreds.isEmpty()) {
            criteriaQuery.where(criteriaBuilder.and(filterPreds.toArray(new Predicate[filterPreds.size()])),
                    criteriaBuilder.or(searchPreds.toArray(new Predicate[searchPreds.size()])));
            countQuery.where(criteriaBuilder.and(filterPreds.toArray(new Predicate[filterPreds.size()])),
                    criteriaBuilder.or(searchPreds.toArray(new Predicate[searchPreds.size()])));
        } else if (!filterPreds.isEmpty()) {
            criteriaQuery.where(filterPreds.toArray(new Predicate[filterPreds.size()]));
            countQuery.where(filterPreds.toArray(new Predicate[filterPreds.size()]));
        } else if (!searchPreds.isEmpty()) {
            criteriaQuery.where(criteriaBuilder.or(searchPreds.toArray(new Predicate[searchPreds.size()])));
            countQuery.where(criteriaBuilder.or(searchPreds.toArray(new Predicate[searchPreds.size()])));
        } else {
            criteriaQuery.where();
            countQuery.where();
        }

        criteriaQuery.orderBy(ords);

        if (pg.getPageSize() == -1) {
            return this.entityManager
                    .createQuery(criteriaQuery)
                    .setFirstResult((pg.getPageNumber() * pg.getPageSize()))
                    .unwrap(Query.class)
                    .stream();
        } else {
            return this.entityManager
                    .createQuery(criteriaQuery)
                    .setFirstResult((pg.getPageNumber() * pg.getPageSize()))
                    .setMaxResults(pg.getPageSize())
                    .unwrap(Query.class)
                    .stream();
        }
    }


}
