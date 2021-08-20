export function userInfo(roles) {
    const values = roles.map((role) => role.split('/').includes('membership#Instructor')) 
    return values.some((value) => value) ? 'Instructor' : 'Learner'
}